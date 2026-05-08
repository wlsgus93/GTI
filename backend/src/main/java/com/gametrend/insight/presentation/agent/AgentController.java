package com.gametrend.insight.presentation.agent;

import com.gametrend.insight.application.agent.AgentRequest;
import com.gametrend.insight.application.agent.AgentResponse;
import com.gametrend.insight.application.agent.AgentService;
import com.gametrend.insight.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 3-Layer Agent REST endpoint — CommandBar / AgentPanel 자연어 query 처리.
 *
 * <p>JWT 인증 필요 (사용자별 채팅 세션).
 *
 * <p>호출 흐름 (룰 95):
 * <ol>
 *   <li>Layer 1: 로컬 분류 (OFF_TOPIC/SMALL_TALK 차단)
 *   <li>Layer 2: 세션 컨텍스트 (꼬리질문)
 *   <li>Layer 3: cloud LLM (Gemini Flash 등)
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "Agent", description = "3-Layer 하이브리드 자연어 query (CommandBar / AgentPanel)")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AgentService service;

    public AgentController(AgentService service) {
        this.service = service;
    }

    @PostMapping("/query")
    @Operation(
            summary = "자연어 query — 3-Layer 라우팅",
            description = "Layer 1 로컬 분류 → OFF_TOPIC/SMALL_TALK 면 hardcoded 응답 (cloud 호출 X). "
                    + "GAME 이면 Layer 2 컨텍스트 + Layer 3 cloud LLM. "
                    + "꼬리질문 (FOLLOW_UP) 은 세션 최근 6턴 자동 포함.")
    public AgentResponse query(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AgentRequest request) {
        return service.handle(user.id(), request);
    }
}
