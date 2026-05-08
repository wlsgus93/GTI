package com.gametrend.insight.application.watchlist;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddWatchlistRequest(
        @NotNull Long gameId,
        @Size(max = 500) String note) {
}
