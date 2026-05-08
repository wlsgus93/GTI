package com.gametrend.insight.application.dimension.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.gametrend.insight.application.economics.Confidence;
import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.application.stats.ZScoreNormalizer;
import com.gametrend.insight.domain.snapshot.MentionSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityDimensionServiceTest {

    @Mock GameJpaRepository gameRepo;
    @Mock MentionSnapshotJpaRepository mentionRepo;
    @Mock RedisCacheTemplate redisCache;

    private CommunityDimensionService service;

    @BeforeEach
    void setUp() {
        service = new CommunityDimensionService(
                gameRepo, mentionRepo, redisCache, new ZScoreNormalizer());
        Mockito.lenient()
                .when(redisCache.get(org.mockito.ArgumentMatchers.anyString(),
                        eq(CommunityDimension.class)))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("미존재 게임 → GameNotFoundException")
    void unknownGame() {
        when(gameRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCommunityDimension(999L))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    @DisplayName("정상 케이스 — 4 mention snapshot 집계 + sentiment ratio")
    void typicalCase() {
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game(1L, "CS2")));
        when(mentionRepo.findByGameId(1L)).thenReturn(List.of(
                mention(SnapshotSource.YOUTUBE, MentionSnapshot.Sentiment.POS, 1000),
                mention(SnapshotSource.YOUTUBE, MentionSnapshot.Sentiment.NEG, 200),
                mention(SnapshotSource.REDDIT, MentionSnapshot.Sentiment.POS, 500),
                mention(SnapshotSource.REDDIT, MentionSnapshot.Sentiment.NEU, 100)));
        // 단일 게임 평균 비교 모집단 부족 → activityZ null
        when(mentionRepo.sumMentionsByGame()).thenReturn(List.<Object[]>of(new Object[]{1L, 1800L}));

        var result = service.getCommunityDimension(1L);

        assertThat(result.gameName()).isEqualTo("CS2");
        assertThat(result.totalMentions()).isEqualTo(1800L); // 1000+200+500+100
        assertThat(result.mentionsByPlatform().get(SnapshotSource.YOUTUBE)).isEqualTo(1200L);
        assertThat(result.mentionsByPlatform().get(SnapshotSource.REDDIT)).isEqualTo(600L);
        // sentiment: POS=1500 / NEU=100 / NEG=200 / ratio = 1500 / 1700 ≈ 0.882
        assertThat(result.sentiment().positive()).isEqualTo(1500L);
        assertThat(result.sentiment().neutral()).isEqualTo(100L);
        assertThat(result.sentiment().negative()).isEqualTo(200L);
        assertThat(result.sentiment().positiveRatio()).isCloseTo(0.882, org.assertj.core.api.Assertions.within(0.001));
    }

    @Test
    @DisplayName("Z-score 활성도 — 5게임 모집단 → 인기게임이 VERY_ACTIVE")
    void activityZScore_fromPopulation() {
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game(1L, "CS2")));
        when(mentionRepo.findByGameId(1L)).thenReturn(List.of(
                mention(SnapshotSource.YOUTUBE, MentionSnapshot.Sentiment.POS, 50000)));
        // 모집단: CS2=50000 (압도적), 다른 게임 ~1000
        when(mentionRepo.sumMentionsByGame()).thenReturn(List.<Object[]>of(
                new Object[]{1L, 50000L},
                new Object[]{2L, 1000L},
                new Object[]{3L, 800L},
                new Object[]{4L, 1500L},
                new Object[]{5L, 700L}));

        var result = service.getCommunityDimension(1L);

        assertThat(result.activityZScore()).isGreaterThan(1.5); // 평균보다 매우 큼
        assertThat(result.activityClass()).isIn("ACTIVE", "VERY_ACTIVE");
    }

    @Test
    @DisplayName("모집단 부족 (< 2 게임) → activityZ null + UNKNOWN class")
    void insufficientPopulation() {
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game(1L, "CS2")));
        when(mentionRepo.findByGameId(1L)).thenReturn(List.of(
                mention(SnapshotSource.YOUTUBE, MentionSnapshot.Sentiment.POS, 1000)));
        when(mentionRepo.sumMentionsByGame()).thenReturn(List.<Object[]>of(new Object[]{1L, 1000L}));

        var result = service.getCommunityDimension(1L);

        assertThat(result.activityZScore()).isNull();
        assertThat(result.activityClass()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("신뢰도 — 5000+ mentions + 80%+ sentiment 분류 → HIGH")
    void confidenceHigh() {
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game(1L, "CS2")));
        when(mentionRepo.findByGameId(1L)).thenReturn(List.of(
                mention(SnapshotSource.YOUTUBE, MentionSnapshot.Sentiment.POS, 5000),
                mention(SnapshotSource.REDDIT, MentionSnapshot.Sentiment.POS, 1500)));
        when(mentionRepo.sumMentionsByGame()).thenReturn(List.<Object[]>of(new Object[]{1L, 6500L}));

        var result = service.getCommunityDimension(1L);
        assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
    }

    @Test
    @DisplayName("신뢰도 — 1000+ but sentiment 분류 부족 → MEDIUM")
    void confidenceMedium() {
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game(1L, "CS2")));
        when(mentionRepo.findByGameId(1L)).thenReturn(List.of(
                mention(SnapshotSource.YOUTUBE, null, 1500))); // sentiment null
        when(mentionRepo.sumMentionsByGame()).thenReturn(List.<Object[]>of(new Object[]{1L, 1500L}));

        var result = service.getCommunityDimension(1L);
        assertThat(result.confidence()).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    @DisplayName("신뢰도 — < 1000 → LOW")
    void confidenceLow() {
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game(1L, "Indie")));
        when(mentionRepo.findByGameId(1L)).thenReturn(List.of(
                mention(SnapshotSource.YOUTUBE, MentionSnapshot.Sentiment.POS, 100)));
        when(mentionRepo.sumMentionsByGame()).thenReturn(List.<Object[]>of(new Object[]{1L, 100L}));

        var result = service.getCommunityDimension(1L);
        assertThat(result.confidence()).isEqualTo(Confidence.LOW);
    }

    @Test
    @DisplayName("Pain Points — V1은 빈 리스트 (LLM 통합 W7+)")
    void painPointsEmptyV1() {
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game(1L, "CS2")));
        when(mentionRepo.findByGameId(1L)).thenReturn(List.of());
        when(mentionRepo.sumMentionsByGame()).thenReturn(List.of());

        var result = service.getCommunityDimension(1L);
        assertThat(result.painPoints()).isEmpty();
        assertThat(result.totalMentions()).isEqualTo(0L);
    }

    // ===== fixtures =====
    private static GameJpaEntity game(long id, String name) {
        try {
            var ctor = GameJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            GameJpaEntity g = ctor.newInstance();
            Field f = GameJpaEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(g, id);
            g.setName(name);
            return g;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MentionSnapshotJpaEntity mention(SnapshotSource source, MentionSnapshot.Sentiment sent, int count) {
        try {
            var ctor = MentionSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            MentionSnapshotJpaEntity e = ctor.newInstance();
            e.setSource(source);
            e.setSentiment(sent);
            e.setMentionCount(count);
            e.setCapturedAt(Instant.now());
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
