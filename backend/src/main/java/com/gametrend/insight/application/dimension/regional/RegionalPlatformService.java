package com.gametrend.insight.application.dimension.regional;

import com.gametrend.insight.application.port.out.AppleChartsPort;
import com.gametrend.insight.application.port.out.GooglePlayChartsPort;
import com.gametrend.insight.domain.dimension.regional.RegionalPlatform;
import com.gametrend.insight.domain.dimension.regional.RegionalPlatform.ChartEntry;
import com.gametrend.insight.domain.dimension.regional.RegionalPlatform.Platform;
import com.gametrend.insight.domain.dimension.regional.RegionalPlatform.PlatformDivergent;
import com.gametrend.insight.domain.dimension.regional.RegionalPlatform.RegionalHit;
import com.gametrend.insight.domain.dimension.regional.RegionalPlatform.Summary;
import com.gametrend.insight.domain.dimension.regional.RegionalPlatform.UniversalHit;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * D7 산출 서비스 — Apple + Google Play 멀티 국가 차트 통합 분석.
 *
 * <p>병렬 호출: N 국가 × 2 플랫폼 = 2N 외부 호출. Virtual Threads 로 wall-clock 단축.
 *
 * <p>매칭 전략: 제목 정규화 (lowercase + alphanumeric only) — 모바일 게임은 보통 글로벌 동일 brand.
 * 일부 국가 한국어/일본어 제목 차이는 매칭 X (TODO: 향후 IGDB 매핑 또는 LLM 정규화).
 *
 * <p>Universal Hit 임계: Top 20 안. Regional Hit: 1 국가만 Top 20. Platform Divergent: 2 플랫폼 비대칭.
 */
@Service
public class RegionalPlatformService {

    private static final Logger log = LoggerFactory.getLogger(RegionalPlatformService.class);

    /** 분석에 쓸 default 국가 (5대 시장). */
    public static final List<String> DEFAULT_COUNTRIES = List.of("us", "kr", "jp", "gb", "de");

    /** 차트 길이 default. */
    public static final int DEFAULT_LIMIT = 50;

    /** Universal Hit 임계 (Top N 안). */
    private static final int UNIVERSAL_TOP_N = 20;

    /** 비-알파벳/숫자 제거용. */
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private final AppleChartsPort applePort;
    private final GooglePlayChartsPort googlePort;

    public RegionalPlatformService(AppleChartsPort applePort, GooglePlayChartsPort googlePort) {
        this.applePort = applePort;
        this.googlePort = googlePort;
    }

    public RegionalPlatform analyze(List<String> countries, int limit) {
        long startNs = System.nanoTime();
        List<String> safeCountries = (countries == null || countries.isEmpty())
                ? DEFAULT_COUNTRIES
                : countries.stream().map(String::toLowerCase).toList();
        int safeLimit = (limit < 10 || limit > 100) ? DEFAULT_LIMIT : limit;

        Map<String, List<ChartEntry>> appleByCountry = new HashMap<>();
        Map<String, List<ChartEntry>> googleByCountry = new HashMap<>();
        int chartsLoaded = 0;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<String, Future<List<ChartEntry>>> appleFutures = new HashMap<>();
            Map<String, Future<List<ChartEntry>>> googleFutures = new HashMap<>();
            for (String country : safeCountries) {
                appleFutures.put(country, executor.submit(() -> fetchApple(country, safeLimit)));
                googleFutures.put(country, executor.submit(() -> fetchGoogle(country, safeLimit)));
            }
            for (String country : safeCountries) {
                List<ChartEntry> appleList = safeFutureGet(appleFutures.get(country), country, "apple");
                List<ChartEntry> googleList = safeFutureGet(googleFutures.get(country), country, "google");
                appleByCountry.put(country, appleList);
                googleByCountry.put(country, googleList);
                if (!appleList.isEmpty()) chartsLoaded++;
                if (!googleList.isEmpty()) chartsLoaded++;
            }
        }

        // 정규화 제목 → 국가/플랫폼 ranking 모음
        Map<String, GameAggregate> aggregates = aggregate(appleByCountry, googleByCountry);

        List<UniversalHit> universalHits = extractUniversalHits(aggregates, safeCountries.size());
        Map<String, List<RegionalHit>> regionalHits = extractRegionalHits(aggregates);
        List<PlatformDivergent> divergent = extractPlatformDivergent(aggregates);

        long latencyMs = (System.nanoTime() - startNs) / 1_000_000;

        return new RegionalPlatform(
                safeCountries,
                List.of(Platform.APPLE, Platform.GOOGLE_PLAY),
                appleByCountry,
                googleByCountry,
                universalHits,
                regionalHits,
                divergent,
                new Summary(safeCountries.size(), chartsLoaded,
                        universalHits.size(), regionalHits.values().stream().mapToInt(List::size).sum(),
                        divergent.size(), latencyMs),
                Instant.now());
    }

    private List<ChartEntry> fetchApple(String country, int limit) {
        return applePort.fetchTopFreeGames(country, limit)
                .map(list -> list.stream()
                        .map(e -> new ChartEntry(e.id(), e.name(), e.artistName(), e.rank()))
                        .toList())
                .orElse(List.of());
    }

    private List<ChartEntry> fetchGoogle(String country, int limit) {
        return googlePort.fetchTopFreeGames(country, limit)
                .map(list -> list.stream()
                        .map(e -> new ChartEntry(e.appId(), e.title(), e.developer(), e.rank()))
                        .toList())
                .orElse(List.of());
    }

    private static List<ChartEntry> safeFutureGet(Future<List<ChartEntry>> f, String country, String platform) {
        try {
            return f.get();
        } catch (Exception e) {
            log.warn("D7 fetch failed: country={}, platform={}, err={}", country, platform, e.getMessage());
            return List.of();
        }
    }

    /** 게임별 (정규화 제목 기준) 국가/플랫폼 ranking 누적. */
    private static Map<String, GameAggregate> aggregate(
            Map<String, List<ChartEntry>> appleByCountry,
            Map<String, List<ChartEntry>> googleByCountry) {
        Map<String, GameAggregate> agg = new HashMap<>();
        appleByCountry.forEach((country, list) -> list.forEach(entry -> {
            String key = normalize(entry.title());
            if (key.isEmpty()) return;
            agg.computeIfAbsent(key, k -> new GameAggregate(entry.title()))
                    .addApple(country, entry.rank());
        }));
        googleByCountry.forEach((country, list) -> list.forEach(entry -> {
            String key = normalize(entry.title());
            if (key.isEmpty()) return;
            agg.computeIfAbsent(key, k -> new GameAggregate(entry.title()))
                    .addGoogle(country, entry.rank());
        }));
        return agg;
    }

    private static String normalize(String title) {
        if (title == null) return "";
        return NON_ALNUM.matcher(title.toLowerCase()).replaceAll("");
    }

    private static List<UniversalHit> extractUniversalHits(Map<String, GameAggregate> agg, int totalCountries) {
        // 임계: 절반 이상 국가에서 Top N 진입 (universal 의미)
        int minCountries = Math.max(2, (totalCountries + 1) / 2);
        return agg.values().stream()
                .map(GameAggregate::toUniversalHit)
                .filter(h -> h != null && h.countriesCovered() >= minCountries)
                .sorted(Comparator.comparingDouble(UniversalHit::avgRank))
                .toList();
    }

    private static Map<String, List<RegionalHit>> extractRegionalHits(Map<String, GameAggregate> agg) {
        Map<String, List<RegionalHit>> result = new HashMap<>();
        for (GameAggregate g : agg.values()) {
            // 단일 국가 + Top N 안만 — 그 외 국가에선 미진입 (regional 편향)
            Map<String, Integer> allRanks = g.allRanks();
            if (allRanks.size() != 1) continue;
            Map.Entry<String, Integer> only = allRanks.entrySet().iterator().next();
            if (only.getValue() > UNIVERSAL_TOP_N) continue;
            Platform plat = g.dominantPlatform();
            result.computeIfAbsent(only.getKey(), k -> new ArrayList<>())
                    .add(new RegionalHit(g.normalizedKey(), g.displayTitle(), only.getValue(), plat));
        }
        result.values().forEach(list -> list.sort(Comparator.comparingInt(RegionalHit::rank)));
        return result;
    }

    private static List<PlatformDivergent> extractPlatformDivergent(Map<String, GameAggregate> agg) {
        // Apple 만 Top N 또는 Google 만 Top N 진입한 게임 (한 country 기준)
        List<PlatformDivergent> result = new ArrayList<>();
        for (GameAggregate g : agg.values()) {
            for (Map.Entry<String, Integer> e : g.appleRanks().entrySet()) {
                if (e.getValue() <= UNIVERSAL_TOP_N
                        && !g.googleRanks().containsKey(e.getKey())) {
                    result.add(new PlatformDivergent(
                            g.normalizedKey(), g.displayTitle(),
                            Platform.APPLE, e.getKey(), e.getValue()));
                }
            }
            for (Map.Entry<String, Integer> e : g.googleRanks().entrySet()) {
                if (e.getValue() <= UNIVERSAL_TOP_N
                        && !g.appleRanks().containsKey(e.getKey())) {
                    result.add(new PlatformDivergent(
                            g.normalizedKey(), g.displayTitle(),
                            Platform.GOOGLE_PLAY, e.getKey(), e.getValue()));
                }
            }
        }
        result.sort(Comparator.comparingInt(PlatformDivergent::rank));
        return result;
    }

    /** 정규화 제목 단위 ranking 누적기. */
    private static final class GameAggregate {
        private final String displayTitle;
        private final Map<String, Integer> appleRanks = new HashMap<>();
        private final Map<String, Integer> googleRanks = new HashMap<>();

        GameAggregate(String displayTitle) {
            this.displayTitle = displayTitle;
        }

        void addApple(String country, int rank) {
            appleRanks.merge(country, rank, Math::min);
        }

        void addGoogle(String country, int rank) {
            googleRanks.merge(country, rank, Math::min);
        }

        Map<String, Integer> appleRanks() { return appleRanks; }
        Map<String, Integer> googleRanks() { return googleRanks; }

        Map<String, Integer> allRanks() {
            Map<String, Integer> merged = new HashMap<>(appleRanks);
            googleRanks.forEach((c, r) -> merged.merge(c, r, Math::min));
            return merged;
        }

        Set<Platform> platforms() {
            Set<Platform> s = new HashSet<>();
            if (!appleRanks.isEmpty()) s.add(Platform.APPLE);
            if (!googleRanks.isEmpty()) s.add(Platform.GOOGLE_PLAY);
            return s;
        }

        Platform dominantPlatform() {
            return appleRanks.size() >= googleRanks.size() ? Platform.APPLE : Platform.GOOGLE_PLAY;
        }

        String normalizedKey() {
            return RegionalPlatformService.normalize(displayTitle);
        }

        String displayTitle() {
            return displayTitle;
        }

        UniversalHit toUniversalHit() {
            Map<String, Integer> all = allRanks();
            // top N 안 진입한 국가만 포함
            Map<String, Integer> topNRanks = new HashMap<>();
            all.forEach((c, r) -> { if (r <= UNIVERSAL_TOP_N) topNRanks.put(c, r); });
            if (topNRanks.isEmpty()) return null;
            double avg = topNRanks.values().stream().mapToInt(Integer::intValue).average().orElse(Double.NaN);
            return new UniversalHit(
                    normalizedKey(), displayTitle, topNRanks, avg,
                    platforms(), topNRanks.size());
        }
    }
}
