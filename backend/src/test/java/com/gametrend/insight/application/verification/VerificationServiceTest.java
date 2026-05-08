package com.gametrend.insight.application.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.gametrend.insight.application.stats.TimeLaggedCorrelation;
import com.gametrend.insight.domain.verification.CampaignStatus;
import com.gametrend.insight.domain.verification.CaseStatus;
import com.gametrend.insight.domain.verification.Platform;
import com.gametrend.insight.domain.verification.StimulusType;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignJpaEntity;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.CampaignMetricJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.StimulusJpaEntity;
import com.gametrend.insight.infrastructure.persistence.verification.StimulusJpaRepository;
import com.gametrend.insight.infrastructure.persistence.verification.VerificationCaseJpaEntity;
import com.gametrend.insight.infrastructure.persistence.verification.VerificationCaseJpaRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock VerificationCaseJpaRepository caseRepo;
    @Mock StimulusJpaRepository stimulusRepo;
    @Mock CampaignJpaRepository campaignRepo;
    @Mock CampaignMetricJpaRepository metricRepo;
    @Mock GameJpaRepository gameRepo;
    @Mock PlayerSnapshotJpaRepository playerSnapshotRepo;

    private VerificationService service;

    @BeforeEach
    void setUp() {
        service = new VerificationService(
                caseRepo, stimulusRepo, campaignRepo, metricRepo,
                gameRepo, playerSnapshotRepo, new TimeLaggedCorrelation());
    }

    @Test
    @DisplayName("listCases — 4 케이스 정렬 + stimulus/campaign 카운트")
    void listCases() {
        var c1 = caseEntity(1L, "C1", "C1 title", CaseStatus.RUNNING, false);
        var c2 = caseEntity(2L, "C2", "C2 title", CaseStatus.RUNNING, false);
        var c3 = caseEntity(3L, "C3", "C3 title", CaseStatus.PLANNING, false);
        var c4 = caseEntity(4L, "C4", "C4 ★", CaseStatus.RUNNING, true);
        // 의도적으로 reverse 순서로 반환 — 서비스가 code 정렬해야
        when(caseRepo.findAll()).thenReturn(List.of(c4, c3, c2, c1));
        when(stimulusRepo.findByCaseIdOrderByCreatedAtAsc(any())).thenReturn(List.of(stimulus(0L, 1L)));
        when(campaignRepo.findByCaseIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(campaignRepo.findByCaseIdOrderByCreatedAtAsc(eq(1L)))
                .thenReturn(List.of(campaign(101L, 1L)));

        List<CaseSummary> result = service.listCases();

        assertThat(result).hasSize(4);
        assertThat(result).extracting(CaseSummary::code).containsExactly("C1", "C2", "C3", "C4");
        assertThat(result.get(3).priority()).isTrue(); // C4
        // C1만 캠페인 1
        assertThat(result.get(0).campaignCount()).isEqualTo(1);
        assertThat(result.get(1).campaignCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("getCaseDetail — 자극물/캠페인 + 누적 KPI")
    void getCaseDetail_withMetrics() {
        var c1 = caseEntity(1L, "C1", "C1 title", CaseStatus.RUNNING, false);
        when(caseRepo.findByCode("C1")).thenReturn(Optional.of(c1));
        when(stimulusRepo.findByCaseIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(stimulus(10L, 1L)));
        when(campaignRepo.findByCaseIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(campaign(20L, 1L)));
        when(metricRepo.aggregateByCampaignIds(List.of(20L)))
                .thenReturn(List.of(new CampaignAggregate(20L, 100_000, 5400, 110, 35_000)));

        CaseDetail detail = service.getCaseDetail("C1");

        assertThat(detail.code()).isEqualTo("C1");
        assertThat(detail.stimuli()).hasSize(1);
        assertThat(detail.campaigns()).hasSize(1);
        var campaign = detail.campaigns().get(0);
        assertThat(campaign.totalImpressions()).isEqualTo(100_000);
        assertThat(campaign.totalClicks()).isEqualTo(5400);
        assertThat(campaign.ctr()).isEqualTo(0.054);
        assertThat(campaign.cvr()).isEqualTo(0.0204); // 110/5400
    }

    @Test
    @DisplayName("getCaseDetail — 메트릭 없는 캠페인 → 0/null KPI")
    void getCaseDetail_noMetrics() {
        var c1 = caseEntity(1L, "C1", "C1", CaseStatus.PLANNING, false);
        when(caseRepo.findByCode("C1")).thenReturn(Optional.of(c1));
        when(stimulusRepo.findByCaseIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(campaignRepo.findByCaseIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(campaign(20L, 1L)));
        when(metricRepo.aggregateByCampaignIds(List.of(20L))).thenReturn(List.of());

        CaseDetail detail = service.getCaseDetail("C1");

        var campaign = detail.campaigns().get(0);
        assertThat(campaign.totalImpressions()).isEqualTo(0);
        assertThat(campaign.ctr()).isNull();
        assertThat(campaign.cvr()).isNull();
    }

    @Test
    @DisplayName("getCaseDetail — 미존재 코드 → VerificationCaseNotFoundException")
    void getCaseDetail_notFound() {
        when(caseRepo.findByCode("C99")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCaseDetail("C99"))
                .isInstanceOf(VerificationCaseNotFoundException.class);
    }

    @Test
    @DisplayName("createStimulus — 정상 등록")
    void createStimulus() {
        var c1 = caseEntity(1L, "C1", "C1", CaseStatus.RUNNING, false);
        when(caseRepo.findByCode("C1")).thenReturn(Optional.of(c1));
        when(stimulusRepo.save(any())).thenAnswer(inv -> {
            StimulusJpaEntity e = inv.getArgument(0);
            // 시뮬레이션: id + createdAt 부여
            try {
                Field f = StimulusJpaEntity.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(e, 100L);
                Field f2 = StimulusJpaEntity.class.getDeclaredField("createdAt");
                f2.setAccessible(true);
                f2.set(e, Instant.now());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return e;
        });

        var req = new StimulusRequest(StimulusType.VIDEO, "Test", "https://x", "desc");
        CaseDetail.StimulusItem result = service.createStimulus("C1", req);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.type()).isEqualTo(StimulusType.VIDEO);
        assertThat(result.title()).isEqualTo("Test");
    }

    @Test
    @DisplayName("createCampaign — stimulusId가 다른 케이스 소속이면 IAE")
    void createCampaign_wrongStimulus_throws() {
        var c1 = caseEntity(1L, "C1", "C1", CaseStatus.RUNNING, false);
        when(caseRepo.findByCode("C1")).thenReturn(Optional.of(c1));
        var stim = stimulus(99L, 999L); // case_id 999 ≠ c1.id 1
        when(stimulusRepo.findById(99L)).thenReturn(Optional.of(stim));

        var req = new CampaignRequest(99L, Platform.META, "name", "utm", 100000);
        assertThatThrownBy(() -> service.createCampaign("C1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stimulusId");
    }

    @Test
    @DisplayName("createCampaign — stimulusId null이면 OK (자극물 미연결 캠페인)")
    void createCampaign_noStimulus() {
        var c1 = caseEntity(1L, "C1", "C1", CaseStatus.PLANNING, false);
        when(caseRepo.findByCode("C1")).thenReturn(Optional.of(c1));
        when(campaignRepo.save(any())).thenAnswer(inv -> {
            CampaignJpaEntity e = inv.getArgument(0);
            try {
                Field f = CampaignJpaEntity.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(e, 200L);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return e;
        });

        var req = new CampaignRequest(null, Platform.YOUTUBE, "name", "utm", 50000);
        CaseDetail.CampaignWithMetrics result = service.createCampaign("C1", req);

        assertThat(result.id()).isEqualTo(200L);
        assertThat(result.status()).isEqualTo(CampaignStatus.SCHEDULED);
        assertThat(result.totalImpressions()).isEqualTo(0);
        assertThat(result.ctr()).isNull();
    }

    private static VerificationCaseJpaEntity caseEntity(long id, String code, String title, CaseStatus status, boolean priority) {
        try {
            var ctor = VerificationCaseJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            var e = ctor.newInstance();
            Field f = VerificationCaseJpaEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, id);
            e.setCode(code);
            e.setTitle(title);
            e.setConcept("concept");
            e.setHypothesis("hypothesis");
            e.setStatus(status);
            e.setPriority(priority);
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static StimulusJpaEntity stimulus(long id, long caseId) {
        StimulusJpaEntity s = StimulusJpaEntity.newInstance(caseId, StimulusType.VIDEO, "title", "url", "desc");
        try {
            Field f = StimulusJpaEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return s;
    }

    private static CampaignJpaEntity campaign(long id, long caseId) {
        CampaignJpaEntity c = CampaignJpaEntity.newInstance(
                caseId, null, Platform.META, "test", "utm", CampaignStatus.RUNNING, 100_000);
        try {
            Field f = CampaignJpaEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return c;
    }
}
