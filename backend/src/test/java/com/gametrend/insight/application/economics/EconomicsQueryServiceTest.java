package com.gametrend.insight.application.economics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PriceSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PriceSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class EconomicsQueryServiceTest {

    @Mock GameJpaRepository gameRepo;
    @Mock PlayerSnapshotJpaRepository playerRepo;
    @Mock PriceSnapshotJpaRepository priceRepo;
    @Mock ViewerSnapshotJpaRepository viewerRepo;
    @Mock MentionSnapshotJpaRepository mentionRepo;
    @Mock com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate redisCache;

    private EconomicsQueryService service;

    @BeforeEach
    void setUp() {
        service = new EconomicsQueryService(
                gameRepo, playerRepo, priceRepo, viewerRepo, mentionRepo,
                new RevenueEstimator(), new UnitEconomicsCalculator(), redisCache);
        // 기본: 캐시 miss (각 테스트는 필요 시 override)
        org.mockito.Mockito.lenient()
                .when(redisCache.get(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.eq(EconomicsInsight.class)))
                .thenReturn(java.util.Optional.empty());
    }

    @Test
    @DisplayName("미존재 게임 → GameNotFoundException")
    void unknown_throws() {
        when(gameRepo.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> service.getEconomicsInsight(999L))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    @DisplayName("Redis L1 hit — repository 호출 최소 (W3 D3)")
    void redisHit_skipsCompute() {
        when(gameRepo.existsById(1L)).thenReturn(true);
        var cached = new EconomicsInsight(
                1L,
                new EconomicsInsight.RevenueEstimate(
                        1_000_000L, 2_000_000L, 1_500_000L,
                        new java.math.BigDecimal("9.99"),
                        new java.math.BigDecimal("14985000.00"),
                        new java.math.BigDecimal("14235750.00"),
                        new java.math.BigDecimal("9965025.00"),
                        80_000, 280_000),
                new EconomicsInsight.UnitEconomics(2.0, 0.1, 1000.0, new java.math.BigDecimal("0.0001")),
                Confidence.HIGH,
                Instant.now());
        when(redisCache.get(org.mockito.ArgumentMatchers.eq("economics:game:1"),
                org.mockito.ArgumentMatchers.eq(EconomicsInsight.class)))
                .thenReturn(Optional.of(cached));

        var result = service.getEconomicsInsight(1L);

        assertThat(result).isSameAs(cached);
        // 캐시 hit이므로 어떤 snapshot repository도 안 부름
        org.mockito.Mockito.verify(playerRepo, org.mockito.Mockito.never()).findLatestWithOwners(anyLong());
        org.mockito.Mockito.verify(priceRepo, org.mockito.Mockito.never())
                .findFirstByGameIdOrderByCapturedAtDesc(anyLong());
        org.mockito.Mockito.verify(redisCache, org.mockito.Mockito.never())
                .put(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Redis miss → 계산 후 1h TTL로 캐시 (W3 D3)")
    void redisMiss_computesAndCaches() {
        when(gameRepo.existsById(1L)).thenReturn(true);
        when(playerRepo.findLatestWithOwners(1L)).thenReturn(Optional.empty());
        when(priceRepo.findFirstByGameIdOrderByCapturedAtDesc(1L)).thenReturn(Optional.empty());
        when(playerRepo.findPeakCcuSince(eq(1L), any())).thenReturn(null);
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class))).thenReturn(List.of());
        when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L)).thenReturn(Optional.empty());
        when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(anyLong(), any()))
                .thenReturn(Optional.empty());

        service.getEconomicsInsight(1L);

        org.mockito.Mockito.verify(redisCache, org.mockito.Mockito.times(1))
                .put(org.mockito.ArgumentMatchers.eq("economics:game:1"),
                        org.mockito.ArgumentMatchers.any(EconomicsInsight.class),
                        org.mockito.ArgumentMatchers.eq(EconomicsQueryService.CACHE_TTL));
    }

    @Test
    @DisplayName("모든 데이터 있음 → revenue + unit + HIGH/MEDIUM 신뢰도")
    void allDataPresent() {
        when(gameRepo.existsById(1L)).thenReturn(true);

        Instant fresh = Instant.now().minusSeconds(3600); // 1h ago

        // SteamSpy owners snapshot — 좁은 범위 (HIGH 후보)
        var ownersSnap = playerSnap(null, null, null, 20_000_000L, 22_000_000L, fresh, SnapshotSource.STEAM_SPY);
        when(playerRepo.findLatestWithOwners(1L)).thenReturn(Optional.of(ownersSnap));

        // 가격
        var priceSnap = priceSnap(5999L, "USD", fresh);
        when(priceRepo.findFirstByGameIdOrderByCapturedAtDesc(1L)).thenReturn(Optional.of(priceSnap));

        // CCU peak
        when(playerRepo.findPeakCcuSince(eq(1L), any())).thenReturn(200_000);

        // 일반 PlayerSnapshot
        var ccuSnap = playerSnap(100_000, 95_000, 100_000, null, null, fresh, SnapshotSource.STEAM);
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(ccuSnap));

        // Twitch
        when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(viewerSnap(50_000, fresh)));

        // Mentions
        when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(eq(1L), eq(SnapshotSource.YOUTUBE)))
                .thenReturn(Optional.of(mentionSnap(10_000, SnapshotSource.YOUTUBE, fresh)));
        when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(eq(1L), eq(SnapshotSource.REDDIT)))
                .thenReturn(Optional.of(mentionSnap(2_500, SnapshotSource.REDDIT, fresh)));

        var insight = service.getEconomicsInsight(1L);

        assertThat(insight.gameId()).isEqualTo(1L);
        assertThat(insight.revenue()).isNotNull();
        assertThat(insight.revenue().ownersMid()).isEqualTo(21_000_000L);
        assertThat(insight.revenue().developerNet()).isNotNull();
        assertThat(insight.revenue().estimatedDau()).isEqualTo(1_600_000);
        assertThat(insight.unitEconomics().viewToPlayRatio()).isEqualTo(2.0);
        // mentions = 12500, ccu = 100K → ratio 0.125
        assertThat(insight.unitEconomics().mentionToPlayRatio()).isEqualTo(0.125);
        // owners 20M~22M, mid 21M, width 2M, ratio 0.095 < 0.30, fresh=true → HIGH
        assertThat(insight.confidence()).isEqualTo(Confidence.HIGH);
    }

    @Test
    @DisplayName("owners 없음 → revenue null, unit은 partial, confidence LOW")
    void ownersAbsent() {
        when(gameRepo.existsById(1L)).thenReturn(true);
        when(playerRepo.findLatestWithOwners(1L)).thenReturn(Optional.empty());
        when(priceRepo.findFirstByGameIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(priceSnap(1499L, "USD", Instant.now())));
        when(playerRepo.findPeakCcuSince(eq(1L), any())).thenReturn(50_000);
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(playerSnap(50_000, null, null, null, null, Instant.now(), SnapshotSource.STEAM)));
        when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L)).thenReturn(Optional.empty());
        when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(anyLong(), any()))
                .thenReturn(Optional.empty());

        var insight = service.getEconomicsInsight(1L);

        assertThat(insight.revenue()).isNull();
        assertThat(insight.unitEconomics().priceEfficiency()).isNotNull(); // CCU + price 있음
        assertThat(insight.confidence()).isEqualTo(Confidence.LOW);
    }

    @Test
    @DisplayName("F2P 게임 — owners 큼, price 0 → revenue 0, priceEfficiency null")
    void f2pGame() {
        when(gameRepo.existsById(1L)).thenReturn(true);
        Instant now = Instant.now();
        when(playerRepo.findLatestWithOwners(1L))
                .thenReturn(Optional.of(playerSnap(null, null, null, 50_000_000L, 100_000_000L, now, SnapshotSource.STEAM_SPY)));
        when(priceRepo.findFirstByGameIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(priceSnap(0L, "USD", now)));
        when(playerRepo.findPeakCcuSince(eq(1L), any())).thenReturn(1_000_000);
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(playerSnap(1_000_000, null, null, null, null, now, SnapshotSource.STEAM)));
        when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(viewerSnap(200_000, now)));
        when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(anyLong(), any()))
                .thenReturn(Optional.empty());

        var insight = service.getEconomicsInsight(1L);

        assertThat(insight.revenue()).isNotNull();
        assertThat(insight.revenue().developerNet()).isEqualByComparingTo("0.00");
        assertThat(insight.unitEconomics().priceEfficiency()).isNull();
        assertThat(insight.unitEconomics().reviewCostPerPositive()).isNull();
        assertThat(insight.unitEconomics().viewToPlayRatio()).isEqualTo(5.0); // 1M/200K
        // 50M~100M, mid 75M, width 50M ratio 0.667 → LOW (가격 있어도 폭이 너무 넓음)
        assertThat(insight.confidence()).isEqualTo(Confidence.LOW);
    }

    @Test
    @DisplayName("스냅샷 stale (24h 이전) → fresh=false → HIGH 후보 → MEDIUM")
    void staleSnapshot_noHigh() {
        when(gameRepo.existsById(1L)).thenReturn(true);
        Instant stale = Instant.now().minusSeconds(48 * 3600); // 48h ago
        // 좁은 범위 (정상이라면 HIGH)
        when(playerRepo.findLatestWithOwners(1L))
                .thenReturn(Optional.of(playerSnap(null, null, null, 100L, 110L, stale, SnapshotSource.STEAM_SPY)));
        when(priceRepo.findFirstByGameIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(priceSnap(1999L, "USD", stale)));
        when(playerRepo.findPeakCcuSince(eq(1L), any())).thenReturn(null);
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of());
        when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L)).thenReturn(Optional.empty());
        when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(anyLong(), any()))
                .thenReturn(Optional.empty());

        var insight = service.getEconomicsInsight(1L);

        // 좁은 범위지만 stale → HIGH 못 받고 MEDIUM
        assertThat(insight.confidence()).isEqualTo(Confidence.MEDIUM);
    }

    // ===== helpers =====
    private static PlayerSnapshotJpaEntity playerSnap(
            Integer ccu, Integer reviewPos, Integer reviewTotal,
            Long ownersLow, Long ownersHigh, Instant at, SnapshotSource source) {
        try {
            var ctor = PlayerSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            var e = ctor.newInstance();
            e.setConcurrentPlayers(ccu);
            e.setReviewScorePositive(reviewPos);
            e.setReviewScoreTotal(reviewTotal);
            e.setOwnersLow(ownersLow);
            e.setOwnersHigh(ownersHigh);
            e.setCapturedAt(at);
            e.setSource(source);
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static PriceSnapshotJpaEntity priceSnap(long cents, String currency, Instant at) {
        try {
            var ctor = PriceSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            var e = ctor.newInstance();
            e.setPriceCents(cents);
            e.setCurrency(currency);
            e.setCapturedAt(at);
            e.setSource(SnapshotSource.STEAM_STORE);
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static ViewerSnapshotJpaEntity viewerSnap(int viewers, Instant at) {
        try {
            var ctor = ViewerSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            var e = ctor.newInstance();
            e.setConcurrentViewers(viewers);
            e.setCapturedAt(at);
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static MentionSnapshotJpaEntity mentionSnap(int count, SnapshotSource src, Instant at) {
        try {
            var ctor = MentionSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            var e = ctor.newInstance();
            e.setMentionCount(count);
            e.setSource(src);
            e.setCapturedAt(at);
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
