package com.gametrend.insight.presentation.insight;

import com.gametrend.insight.application.insight.GameInsight;
import com.gametrend.insight.application.insight.InsightService;
import com.gametrend.insight.application.insight.MultiPersonaInsight;
import com.gametrend.insight.domain.insight.Persona;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * P2 게임 상세 — AI 인사이트 탭 (W2 Day 5).
 *
 * <p>GET 호출 시:
 * <ul>
 *   <li>DB 캐시 hit (24h TTL) → 즉시 반환 ({@code cached=true})
 *   <li>miss → 게임 데이터 조립 + LLM 호출 + 영속화 → 반환 ({@code cached=false})
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Game Detail", description = "P2 게임 상세 — AI 인사이트")
public class InsightController {

    private final InsightService service;

    public InsightController(InsightService service) {
        this.service = service;
    }

    @GetMapping("/{id}/insight")
    @Operation(
            summary = "AI 1분 요약 인사이트 (페르소나 분기)",
            description =
                    "Claude로 생성하는 한국어 시장 분석 요약. "
                            + "GameDetail + Player + Economics 데이터를 종합. "
                            + "페르소나별 톤 + Strategy 형태 분기 — INDIE/PUBLISHER/MARKETER/INVESTOR. "
                            + "Default = INDIE (검은토끼흰토끼 도메인 fit). "
                            + "캐시 페르소나별 분리 (24h TTL).")
    public GameInsight getInsight(
            @PathVariable long id,
            @Parameter(description = "응답 페르소나 (default: INDIE)")
                    @RequestParam(required = false, defaultValue = "INDIE") Persona persona) {
        return service.getOrGenerate(id, persona);
    }

    @GetMapping("/{id}/insights")
    @Operation(
            summary = "Multi-persona AI 인사이트 (W6 D2)",
            description =
                    "여러 페르소나 동시 호출 — Virtual Threads 병렬. "
                            + "각 페르소나는 독립 fallback chain (Redis → DB → LLM → stale). "
                            + "캐시 isolation으로 4 페르소나 동시 캐시 가능. "
                            + "Wall-clock = max(per-persona latency) ≈ ~2ms (모두 L1 hit 시). "
                            + "응답에 totalLatencyMs 포함 (정량 지표 client 검증용).")
    public MultiPersonaInsight getMultiInsight(
            @PathVariable long id,
            @Parameter(description = "페르소나 1~4개 (콤마 구분, default: INDIE)")
                    @RequestParam(name = "personas", required = false) List<Persona> personas) {
        if (personas == null || personas.isEmpty()) {
            personas = List.of(Persona.DEFAULT);
        }
        return service.getOrGenerateMulti(id, personas);
    }
}
