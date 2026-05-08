package com.gametrend.insight.presentation.dimension;

import com.gametrend.insight.application.dimension.community.CommunityDimension;
import com.gametrend.insight.application.dimension.community.CommunityDimensionService;
import com.gametrend.insight.application.dimension.regional.RegionalPlatformService;
import com.gametrend.insight.application.dimension.release.ReleaseDimension;
import com.gametrend.insight.application.dimension.release.ReleaseDimensionService;
import com.gametrend.insight.domain.dimension.regional.RegionalPlatform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 7차원 분석 REST.
 *
 * <p>구현 차원: D1 (W3 D5), D2 (트렌드 보드, /api/v1/trends), D5 (W6 D3 — 커뮤니티 활성도),
 * D7 (W9 — 지역·플랫폼).
 * <p>D3, D4, D6은 향후 구현.
 */
@RestController
@RequestMapping("/api/v1/dimensions")
@Tag(name = "Dimensions", description = "7차원 분석")
public class DimensionController {

    private final ReleaseDimensionService releaseDimensionService;
    private final CommunityDimensionService communityDimensionService;
    private final RegionalPlatformService regionalPlatformService;

    public DimensionController(
            ReleaseDimensionService releaseDimensionService,
            CommunityDimensionService communityDimensionService,
            RegionalPlatformService regionalPlatformService) {
        this.releaseDimensionService = releaseDimensionService;
        this.communityDimensionService = communityDimensionService;
        this.regionalPlatformService = regionalPlatformService;
    }

    @GetMapping("/d1-release")
    @Operation(
            summary = "D1 출시 동향 — 장르별/연도별 집계",
            description = "각 장르의 게임 수 + 평균/최대 CCU + Top 게임 + Hit/Flop 분류. "
                    + "각 연도의 출시 게임 수 + 평균 CCU. 30분 Redis 캐시.")
    public ReleaseDimension getReleaseDimension() {
        return releaseDimensionService.getReleaseDimension();
    }

    @GetMapping("/d5-community/{gameId}")
    @Operation(
            summary = "D5 커뮤니티 활성도 — 게임별 mention 집계 + sentiment 분포 + 활성도 Z-score",
            description = "Reddit + YouTube mention_snapshot 활용. "
                    + "활성도는 전체 게임 평균 대비 Z-score (±1σ 기준 VERY_ACTIVE/ACTIVE/NORMAL/QUIET/VERY_QUIET 분류). "
                    + "Pain Point 추출은 V1 빈 리스트 (LLM Sentiment 통합은 W7+).")
    public CommunityDimension getCommunityDimension(@PathVariable long gameId) {
        return communityDimensionService.getCommunityDimension(gameId);
    }

    @GetMapping("/d7-regional")
    @Operation(
            summary = "D7 지역·플랫폼 — Apple + Google Play 멀티 국가 차트 통합 분석",
            description = "N 국가 × 2 플랫폼 차트 병렬 호출 (Virtual Threads). "
                    + "Universal Hits (전 국가 인기) / Regional Hits (특정 국가만 인기) / "
                    + "Platform Divergent (Apple ↔ Google 비대칭) 자동 추출. "
                    + "제목 정규화 (lowercase + alphanumeric) 로 cross-country 매칭. LLM 사용 X.")
    public RegionalPlatform getRegionalDimension(
            @RequestParam(name = "countries", defaultValue = "us,kr,jp,gb,de") String countries,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        List<String> list = Arrays.stream(countries.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return regionalPlatformService.analyze(list, limit);
    }
}
