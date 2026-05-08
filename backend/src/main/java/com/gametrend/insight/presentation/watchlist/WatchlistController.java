package com.gametrend.insight.presentation.watchlist;

import com.gametrend.insight.application.watchlist.AddWatchlistRequest;
import com.gametrend.insight.application.watchlist.WatchlistItem;
import com.gametrend.insight.application.watchlist.WatchlistService;
import com.gametrend.insight.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * P4 워치리스트 REST. 모든 endpoint는 JWT 인증 필요 (SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/watchlist")
@Tag(name = "Watchlist (P4)", description = "사용자 게임 워치리스트 — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
public class WatchlistController {

    private final WatchlistService service;

    public WatchlistController(WatchlistService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "내 워치리스트 — 게임 메타 + 최신 CCU 포함")
    public List<WatchlistItem> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "게임 추가",
            description = "이미 워치리스트에 있는 게임이면 400, 미존재 게임이면 422")
    public WatchlistItem add(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddWatchlistRequest req) {
        return service.add(user.id(), req);
    }

    @DeleteMapping("/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "게임 제거", description = "워치리스트에 없으면 400")
    public void remove(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long gameId) {
        service.remove(user.id(), gameId);
    }
}
