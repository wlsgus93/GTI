package com.gametrend.insight.domain.game;

import java.time.Instant;

public record Genre(Long id, String name, Instant createdAt) {

    public Genre {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Genre name must not be blank");
        }
    }
}
