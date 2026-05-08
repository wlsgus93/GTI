package com.gametrend.insight.presentation.exception;

import com.gametrend.insight.application.auth.AuthException;
import com.gametrend.insight.application.insight.LlmUnavailableException;
import com.gametrend.insight.domain.common.DomainException;
import com.gametrend.insight.infrastructure.external.common.ExternalApiException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 → RFC 7807 ProblemDetail 응답 매핑.
 *
 * <p>도메인 예외, 외부 API 예외, 검증 실패를 일관된 형식으로 반환.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_TYPE_BASE = "https://gametrend.insight/errors/";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex, HttpServletRequest req) {
        log.warn("Domain exception: code={} message={}", ex.errorCode(), ex.getMessage());
        ProblemDetail problem = build(HttpStatus.UNPROCESSABLE_ENTITY, ex.errorCode(), ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    /** 인증 실패 — 401. AuthException을 별도로 매핑 (DomainException 422 위로). */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> handleAuth(AuthException ex, HttpServletRequest req) {
        log.warn("Auth failed: {}", ex.getMessage());
        ProblemDetail problem = build(HttpStatus.UNAUTHORIZED, ex.errorCode(), ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(ExternalApiException.Client.class)
    public ResponseEntity<ProblemDetail> handleExternalClient(
            ExternalApiException.Client ex, HttpServletRequest req) {
        log.warn("External API client error: source={} status={} message={}",
                ex.source(), ex.status(), ex.getMessage());
        ProblemDetail problem = build(HttpStatus.BAD_GATEWAY, "external-api-client", ex.getMessage(), req);
        problem.setProperty("source", ex.source());
        problem.setProperty("upstreamStatus", ex.status());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    @ExceptionHandler(ExternalApiException.Server.class)
    public ResponseEntity<ProblemDetail> handleExternalServer(
            ExternalApiException.Server ex, HttpServletRequest req) {
        log.error("External API server error: source={} message={}", ex.source(), ex.getMessage(), ex);
        ProblemDetail problem = build(HttpStatus.SERVICE_UNAVAILABLE, "external-api-server", ex.getMessage(), req);
        problem.setProperty("source", ex.source());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(ExternalApiException.RateLimit.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(
            ExternalApiException.RateLimit ex, HttpServletRequest req) {
        log.warn("External API rate limit: source={} retryAfterMs={}", ex.source(), ex.retryAfterMs());
        ProblemDetail problem = build(HttpStatus.TOO_MANY_REQUESTS, "external-api-rate-limit", ex.getMessage(), req);
        problem.setProperty("source", ex.source());
        problem.setProperty("retryAfterMs", ex.retryAfterMs());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage()))
                .toList();
        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "validation", "Request body validation failed", req);
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(
            ConstraintViolationException ex, HttpServletRequest req) {
        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "validation", ex.getMessage(), req);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * 잘못된 query param 타입 — enum conversion 실패 등 → 400.
     * W6 D2 도입 (예: {@code ?persona=INVALID_PERSONA}).
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex,
            HttpServletRequest req) {
        String paramName = ex.getName();
        String value = ex.getValue() == null ? "null" : ex.getValue().toString();
        String detail = String.format("Invalid value '%s' for parameter '%s'", value, paramName);
        log.warn("Type mismatch: {}", detail);
        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "bad-request", detail, req);
        problem.setProperty("parameter", paramName);
        problem.setProperty("invalidValue", value);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * 컨트롤러에서 잘못된 쿼리 파라미터 등으로 던진 IAE → 400.
     * 예: {@code CcuRange.parse("invalid")}, 도메인 검증 실패.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Bad request: {}", ex.getMessage());
        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "bad-request", ex.getMessage(), req);
        return ResponseEntity.badRequest().body(problem);
    }

    /** LLM 호출 실패 + stale fallback 도 없음 → 503. W3 D1 도입. */
    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleLlmUnavailable(
            LlmUnavailableException ex, HttpServletRequest req) {
        log.error("LLM unavailable: gameId={}", ex.getGameId(), ex);
        ProblemDetail problem = build(
                HttpStatus.SERVICE_UNAVAILABLE, "llm-unavailable",
                "LLM 호출 실패 + 캐시된 분석도 없음. 잠시 후 재시도.", req);
        problem.setProperty("gameId", ex.getGameId());
        problem.setProperty("retryAfterSeconds", 60);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "60").body(problem);
    }

    /**
     * 회로 차단기 OPEN 상태에서 stale fallback도 못 한 경우 (이론상 InsightService에서 LlmUnavailable로 변환되지만
     * 다른 경로에서 새어 나올 수 있어 명시적 매핑).
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemDetail> handleCircuitOpen(
            CallNotPermittedException ex, HttpServletRequest req) {
        log.warn("Circuit breaker OPEN: {}", ex.getMessage());
        ProblemDetail problem = build(
                HttpStatus.SERVICE_UNAVAILABLE, "circuit-open",
                "회로 차단기 OPEN — 다운스트림 일시 차단됨", req);
        problem.setProperty("circuitName", ex.getCausingCircuitBreakerName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "60").body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = build(
                HttpStatus.INTERNAL_SERVER_ERROR, "internal", "Internal server error", req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail build(HttpStatus status, String code, String detail, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(ERROR_TYPE_BASE + code));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
