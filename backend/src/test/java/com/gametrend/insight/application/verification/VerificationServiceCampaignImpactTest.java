package com.gametrend.insight.application.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.gametrend.insight.application.economics.Confidence;
import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.application.stats.TimeLaggedCorrelation;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.domain.verification.CampaignStatus;
import com.gametrend.insight.domain.verification.Platform;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignJpaEntity;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignMetricJpaEntity;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignMetricJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.StimulusJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.VerificationCaseJpaRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * VerificationService.analyzeCampaignImpact 단위 테스트 (W7 D2).
 *
 * <p>시차 상관: 캠페인 일별 클릭수 vs 게임 일별 max CCU.
 *
 * <p>JPA 엔티티 protected 생성자는 reflection으로 우회 (기존 VerificationServiceTest 패턴 재사용).
 */
@ExtendWith(MockitoExtension.class)
class VerificationServiceCampaignImpactTest {

    @Mock VerificationCaseJpaRepository caseRepo;
    @Mock StimulusJpaRepository stimulusRepo;
    @Mock CampaignJpaRepository campaignRepo;
    @Mock CampaignMetricJpaRepository metricRepo;
    @Mock GameJpaRepository gameRepo;
    @Mock PlayerSnapshotJpaRepository playerSnapshotRepo;

    private VerificationService service;

    private static final long CAMPAIGN_ID = 100L;
    private static final long GAME_ID = 1L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 4, 1);

    @BeforeEach
    void setUp() {
        service = new VerificationService(
                caseRepo, stimulusRepo, campaignRepo, metricRepo,
                gameRepo, playerSnapshotRepo, new TimeLaggedCorrelation());
    }

    @Test
    @DisplayName("정상 — 강한 시차 상관 (lag=2) → 자연어 해석 + HIGH confidence")
    void analyze_strongLaggedCorrelation() {
        wireCampaign(START_DATE, START_DATE.plusDays(29));
        when(gameRepo.existsById(GAME_ID)).thenReturn(true);

        // 클릭 (lag=0): 0,1,2,...,29
        // CCU (lag=2): 0,0,0,100,200,...,2700 — 클릭이 2일 후 영향
        List<CampaignMetricJpaEntity> metrics = new ArrayList<>();
        List<PlayerSnapshotJpaEntity> snapshots = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            metrics.add(metric(START_DATE.plusDays(i), i));
            snapshots.add(snapshot(START_DATE.plusDays(i), i >= 2 ? (i - 2) * 100 : 0));
        }
        when(metricRepo.findByCampaignIdOrderByCapturedAtAsc(CAMPAIGN_ID)).thenReturn(metrics);
        when(playerSnapshotRepo.findAll()).thenReturn(snapshots);

        var result = service.analyzeCampaignImpact(CAMPAIGN_ID, GAME_ID, 14);

        assertThat(result.hasResult()).isTrue();
        assertThat(result.bestLagDays()).isEqualTo(2);
        assertThat(result.bestCorrelation()).isGreaterThan(0.95);
        assertThat(result.sampleSize()).isEqualTo(30);
        assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(result.interpretation())
                .contains("t+2일")
                .contains("강한")
                .contains("양의")
                .contains("상관 ≠ 인과");
        assertThat(result.correlationsByLag()).containsKey(0).containsKey(2).containsKey(14);
    }

    @Test
    @DisplayName("Sample 부족 — 윈도우 4일 → null + LOW confidence + sample 부족 메시지")
    void analyze_insufficientSamples() {
        wireCampaign(START_DATE, START_DATE.plusDays(3)); // 4일 윈도우 (MIN_SAMPLE_SIZE=5 미달)
        when(gameRepo.existsById(GAME_ID)).thenReturn(true);
        when(metricRepo.findByCampaignIdOrderByCapturedAtAsc(CAMPAIGN_ID)).thenReturn(List.of());
        when(playerSnapshotRepo.findAll()).thenReturn(List.of());

        var result = service.analyzeCampaignImpact(CAMPAIGN_ID, GAME_ID, 14);

        assertThat(result.hasResult()).isFalse();
        assertThat(result.bestLagDays()).isNull();
        assertThat(result.bestCorrelation()).isNull();
        assertThat(result.confidence()).isEqualTo(Confidence.LOW);
        assertThat(result.interpretation()).contains("Sample 부족");
    }

    @Test
    @DisplayName("maxLagDays 0 → IllegalArgumentException")
    void analyze_invalidMaxLag_zero() {
        assertThatThrownBy(() -> service.analyzeCampaignImpact(CAMPAIGN_ID, GAME_ID, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxLagDays must be 1~30");
    }

    @Test
    @DisplayName("maxLagDays 31 → IllegalArgumentException")
    void analyze_invalidMaxLag_tooLarge() {
        assertThatThrownBy(() -> service.analyzeCampaignImpact(CAMPAIGN_ID, GAME_ID, 31))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Campaign 미존재 → IllegalArgumentException")
    void analyze_campaignNotFound() {
        when(campaignRepo.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyzeCampaignImpact(CAMPAIGN_ID, GAME_ID, 14))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Campaign not found");
    }

    @Test
    @DisplayName("Campaign 미시작 (startedAt null) → IllegalArgumentException")
    void analyze_campaignNotStarted() {
        var campaign = newCampaign();
        setField(campaign, "id", CAMPAIGN_ID);
        campaign.setName("테스트");
        campaign.setPlatform(Platform.META);
        campaign.setStatus(CampaignStatus.SCHEDULED);
        // startedAt = null
        when(campaignRepo.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.analyzeCampaignImpact(CAMPAIGN_ID, GAME_ID, 14))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Campaign not started");
    }

    @Test
    @DisplayName("Game 미존재 → GameNotFoundException")
    void analyze_gameNotFound() {
        wireCampaign(START_DATE, START_DATE.plusDays(29));
        when(gameRepo.existsById(GAME_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.analyzeCampaignImpact(CAMPAIGN_ID, GAME_ID, 14))
                .isInstanceOf(GameNotFoundException.class);
    }

    // ===== fixtures (reflection 우회 — JPA protected 생성자) =====

    private void wireCampaign(LocalDate startDate, LocalDate endDate) {
        var campaign = newCampaign();
        setField(campaign, "id", CAMPAIGN_ID);
        campaign.setName("Twitch Pre-launch — C4");
        campaign.setPlatform(Platform.META);
        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setStartedAt(startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        campaign.setEndedAt(endDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        when(campaignRepo.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
    }

    private static CampaignMetricJpaEntity metric(LocalDate date, long clicks) {
        var m = newInstance(CampaignMetricJpaEntity.class);
        m.setCampaignId(CAMPAIGN_ID);
        m.setCapturedAt(date.atStartOfDay(ZoneOffset.UTC).toInstant());
        m.setImpressions(clicks * 10);
        m.setClicks(clicks);
        m.setConversions(0);
        m.setSpentCents(0);
        return m;
    }

    private static PlayerSnapshotJpaEntity snapshot(LocalDate date, int ccu) {
        var s = newInstance(PlayerSnapshotJpaEntity.class);
        s.setGameId(GAME_ID);
        s.setConcurrentPlayers(ccu);
        s.setCapturedAt(date.atStartOfDay(ZoneOffset.UTC).plusSeconds(3600).toInstant());
        s.setSource(SnapshotSource.STEAM);
        s.setStale(false);
        return s;
    }

    private static CampaignJpaEntity newCampaign() {
        return newInstance(CampaignJpaEntity.class);
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
