package com.gametrend.insight.application.verification;

import com.gametrend.insight.application.economics.Confidence;
import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.application.stats.TimeLaggedCorrelation;
import com.gametrend.insight.domain.verification.CampaignStatus;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignJpaEntity;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignMetricJpaEntity;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignMetricJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.StimulusJpaEntity;
import com.gametrend.insight.infrastructure.persistence.verification.StimulusJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.VerificationCaseJpaEntity;
import com.gametrend.insight.infrastructure.persistence.verification.VerificationCaseJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P7 검증 모듈 서비스 — 4 케이스 + 자극물/캠페인/메트릭 통합.
 *
 * <p>책임:
 * <ul>
 *   <li>{@link #listCases()} — 4 케이스 요약 (자극물/캠페인 카운트 포함)
 *   <li>{@link #getCaseDetail(String)} — 단일 케이스 + 자극물 + 캠페인 (KPI 포함)
 *   <li>{@link #createStimulus} — 자극물 등록
 *   <li>{@link #createCampaign} — 캠페인 시작
 * </ul>
 *
 * <p>KPI (CTR/CVR/CPM/CPC)는 {@link CampaignAggregate}에서 계산.
 */
@Service
public class VerificationService {

    /** 시차 상관 분석 default lag window (일). */
    public static final int DEFAULT_MAX_LAG_DAYS = 14;

    private final VerificationCaseJpaRepository caseRepo;
    private final StimulusJpaRepository stimulusRepo;
    private final CampaignJpaRepository campaignRepo;
    private final CampaignMetricJpaRepository metricRepo;
    private final GameJpaRepository gameRepo;
    private final PlayerSnapshotJpaRepository playerSnapshotRepo;
    private final TimeLaggedCorrelation timeLaggedCorrelation;

    public VerificationService(
            VerificationCaseJpaRepository caseRepo,
            StimulusJpaRepository stimulusRepo,
            CampaignJpaRepository campaignRepo,
            CampaignMetricJpaRepository metricRepo,
            GameJpaRepository gameRepo,
            PlayerSnapshotJpaRepository playerSnapshotRepo,
            TimeLaggedCorrelation timeLaggedCorrelation) {
        this.caseRepo = caseRepo;
        this.stimulusRepo = stimulusRepo;
        this.campaignRepo = campaignRepo;
        this.metricRepo = metricRepo;
        this.gameRepo = gameRepo;
        this.playerSnapshotRepo = playerSnapshotRepo;
        this.timeLaggedCorrelation = timeLaggedCorrelation;
    }

    @Transactional(readOnly = true)
    public List<CaseSummary> listCases() {
        List<VerificationCaseJpaEntity> cases = caseRepo.findAll();
        return cases.stream()
                .sorted(Comparator.comparing(VerificationCaseJpaEntity::getCode))
                .map(c -> new CaseSummary(
                        c.getId(),
                        c.getCode(),
                        c.getTitle(),
                        c.getStatus(),
                        c.isPriority(),
                        stimulusRepo.findByCaseIdOrderByCreatedAtAsc(c.getId()).size(),
                        campaignRepo.findByCaseIdOrderByCreatedAtAsc(c.getId()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseDetail getCaseDetail(String code) {
        VerificationCaseJpaEntity caseE = caseRepo.findByCode(code)
                .orElseThrow(() -> new VerificationCaseNotFoundException(code));

        List<StimulusJpaEntity> stimuli = stimulusRepo.findByCaseIdOrderByCreatedAtAsc(caseE.getId());
        List<CampaignJpaEntity> campaigns = campaignRepo.findByCaseIdOrderByCreatedAtAsc(caseE.getId());

        // 캠페인별 누적 메트릭 일괄 조회 (N+1 회피)
        Map<Long, CampaignAggregate> aggMap = campaigns.isEmpty()
                ? Map.of()
                : metricRepo.aggregateByCampaignIds(
                        campaigns.stream().map(CampaignJpaEntity::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(CampaignAggregate::campaignId, a -> a));

        List<CaseDetail.StimulusItem> stimulusItems = stimuli.stream()
                .map(s -> new CaseDetail.StimulusItem(
                        s.getId(), s.getType(), s.getTitle(), s.getUrl(), s.getDescription(), s.getCreatedAt()))
                .toList();

        List<CaseDetail.CampaignWithMetrics> campaignItems = campaigns.stream()
                .map(c -> {
                    CampaignAggregate agg = aggMap.getOrDefault(c.getId(),
                            new CampaignAggregate(c.getId(), 0, 0, 0, 0));
                    return new CaseDetail.CampaignWithMetrics(
                            c.getId(), c.getStimulusId(), c.getPlatform(), c.getName(), c.getUtmCampaign(),
                            c.getStatus(), c.getStartedAt(), c.getEndedAt(), c.getBudgetCents(), c.getSpentCents(),
                            agg.totalImpressions(), agg.totalClicks(), agg.totalConversions(),
                            agg.ctr(), agg.cvr(), agg.cpmCents(), agg.cpcCents());
                })
                .toList();

        return new CaseDetail(
                caseE.getId(), caseE.getCode(), caseE.getTitle(),
                caseE.getConcept(), caseE.getHypothesis(), caseE.getTargetPersona(),
                caseE.getStatus(), caseE.isPriority(),
                caseE.getCreatedAt(), caseE.getUpdatedAt(),
                stimulusItems, campaignItems);
    }

    @Transactional
    public CaseDetail.StimulusItem createStimulus(String code, StimulusRequest req) {
        VerificationCaseJpaEntity caseE = caseRepo.findByCode(code)
                .orElseThrow(() -> new VerificationCaseNotFoundException(code));

        StimulusJpaEntity entity = StimulusJpaEntity.newInstance(
                caseE.getId(), req.type(), req.title(), req.url(), req.description());

        StimulusJpaEntity saved = stimulusRepo.save(entity);
        return new CaseDetail.StimulusItem(
                saved.getId(), saved.getType(), saved.getTitle(), saved.getUrl(),
                saved.getDescription(), saved.getCreatedAt());
    }

    @Transactional
    public CaseDetail.CampaignWithMetrics createCampaign(String code, CampaignRequest req) {
        VerificationCaseJpaEntity caseE = caseRepo.findByCode(code)
                .orElseThrow(() -> new VerificationCaseNotFoundException(code));

        // stimulusId 검증 (제공된 경우 같은 케이스에 속하는지)
        if (req.stimulusId() != null) {
            stimulusRepo.findById(req.stimulusId())
                    .filter(s -> s.getCaseId().equals(caseE.getId()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "stimulusId " + req.stimulusId() + " does not belong to case " + code));
        }

        CampaignJpaEntity entity = CampaignJpaEntity.newInstance(
                caseE.getId(), req.stimulusId(), req.platform(), req.name(),
                req.utmCampaign(), CampaignStatus.SCHEDULED, req.budgetCents());

        CampaignJpaEntity saved = campaignRepo.save(entity);
        return new CaseDetail.CampaignWithMetrics(
                saved.getId(), saved.getStimulusId(), saved.getPlatform(), saved.getName(),
                saved.getUtmCampaign(), saved.getStatus(), saved.getStartedAt(), saved.getEndedAt(),
                saved.getBudgetCents(), saved.getSpentCents(),
                0, 0, 0, null, null, null, null);
    }

    // ====================================================================================
    // W7 D2 — 시차 상관 분석 (마케터 페르소나 핵심)
    // ====================================================================================

    /**
     * 캠페인 → CCU 시차 상관 분석.
     *
     * <p>캠페인 일별 클릭수 vs 게임 일별 max CCU의 Pearson 상관을 lag 0~maxLag에서 측정.
     * 자연어 해석 + 신뢰도 함께 응답.
     *
     * <p>한계: 상관 ≠ 인과. 다른 요인 (Steam 세일/이벤트) 영향 가능. 룰 safety.md.
     *
     * @param campaignId  분석 대상 캠페인
     * @param gameId      비교할 게임 (캠페인이 어느 게임의 가설을 검증하는지 명시)
     * @param maxLagDays  검색할 최대 lag (1~30, default 14)
     * @throws IllegalArgumentException campaign 미시작 / gameId 미존재
     */
    @Transactional(readOnly = true)
    public CampaignImpactAnalysis analyzeCampaignImpact(long campaignId, long gameId, int maxLagDays) {
        if (maxLagDays < 1 || maxLagDays > 30) {
            throw new IllegalArgumentException("maxLagDays must be 1~30, got " + maxLagDays);
        }

        CampaignJpaEntity campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: id=" + campaignId));
        if (campaign.getStartedAt() == null) {
            throw new IllegalArgumentException("Campaign not started: id=" + campaignId);
        }
        if (!gameRepo.existsById(gameId)) {
            throw new GameNotFoundException(gameId);
        }

        // 분석 윈도우 — 캠페인 시작 ~ ended_at 또는 시작+30일
        LocalDate from = campaign.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate to = campaign.getEndedAt() != null
                ? campaign.getEndedAt().atZone(ZoneOffset.UTC).toLocalDate()
                : from.plusDays(30);

        // 일별 aggregation
        Map<LocalDate, Long> dailyClicks = aggregateCampaignClicksDaily(campaignId);
        Map<LocalDate, Long> dailyCcu = aggregateCcuDaily(gameId);

        // align — 같은 날짜 시리즈
        List<LocalDate> dates = from.datesUntil(to.plusDays(1)).toList();
        double[] x = dates.stream().mapToDouble(d -> dailyClicks.getOrDefault(d, 0L)).toArray();
        double[] y = dates.stream().mapToDouble(d -> dailyCcu.getOrDefault(d, 0L)).toArray();

        int sampleSize = x.length;

        // 시차 상관 (TimeLaggedCorrelation — W5 D3)
        var bestLag = timeLaggedCorrelation.findBestLag(x, y, maxLagDays);

        Integer bestLagDays = bestLag != null ? bestLag.bestLag() : null;
        Double bestCorr = bestLag != null ? bestLag.bestCorrelation() : null;
        Map<Integer, Double> correlationsByLag = bestLag != null ? bestLag.correlationsByLag() : Map.of();

        String interpretation = buildInterpretation(bestLag, sampleSize);
        Confidence confidence = assessLagConfidence(sampleSize);

        return new CampaignImpactAnalysis(
                campaignId,
                gameId,
                campaign.getName(),
                bestLagDays,
                bestCorr,
                correlationsByLag,
                sampleSize,
                from,
                to,
                interpretation,
                confidence,
                Instant.now());
    }

    /** campaign_metric → 일별 clicks 합계. */
    private Map<LocalDate, Long> aggregateCampaignClicksDaily(long campaignId) {
        List<CampaignMetricJpaEntity> metrics = metricRepo.findByCampaignIdOrderByCapturedAtAsc(campaignId);
        return metrics.stream().collect(Collectors.groupingBy(
                m -> m.getCapturedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                Collectors.summingLong(CampaignMetricJpaEntity::getClicks)));
    }

    /** player_snapshot → 일별 max CCU. */
    private Map<LocalDate, Long> aggregateCcuDaily(long gameId) {
        List<PlayerSnapshotJpaEntity> snapshots = playerSnapshotRepo.findAll().stream()
                .filter(s -> s.getGameId() != null && s.getGameId() == gameId)
                .filter(s -> s.getConcurrentPlayers() != null)
                .toList();
        return snapshots.stream().collect(Collectors.groupingBy(
                s -> s.getCapturedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparingInt(PlayerSnapshotJpaEntity::getConcurrentPlayers)),
                        opt -> opt.map(s -> (long) (int) s.getConcurrentPlayers()).orElse(0L))));
    }

    /** 자연어 해석 — 마케터 페르소나용. */
    private static String buildInterpretation(
            TimeLaggedCorrelation.BestLagResult bestLag, int sampleSize) {
        if (bestLag == null) {
            return String.format(
                    "Sample 부족 (n=%d, min %d+lag) — 시차 상관 계산 불가. 캠페인 운영 기간 확장 + 일별 데이터 누적 필요.",
                    sampleSize, TimeLaggedCorrelation.MIN_SAMPLE_SIZE);
        }
        double r = bestLag.bestCorrelation();
        int lag = bestLag.bestLag();
        String strength = Math.abs(r) > 0.7 ? "강한"
                : Math.abs(r) > 0.4 ? "중간"
                : "약한";
        String direction = r >= 0 ? "양의" : "음의";
        return String.format(
                "캠페인 시작 t+%d일 후 CCU에 %s %s 시차 상관 (r=%.2f, n=%d). 상관 ≠ 인과 — 다른 요인(이벤트/세일) 통제 후 추가 검증 권고.",
                lag, strength, direction, r, sampleSize);
    }

    private static Confidence assessLagConfidence(int sampleSize) {
        if (sampleSize >= 30) return Confidence.HIGH;
        if (sampleSize >= 14) return Confidence.MEDIUM;
        return Confidence.LOW;
    }
}
