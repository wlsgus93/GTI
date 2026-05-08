package com.gametrend.insight.domain.user;

import java.time.Instant;

public record User(
        Long id,
        String email,
        String passwordHash,
        String displayName,
        Role role,
        Instant createdAt) {

    public User {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (role == null) {
            role = Role.USER;
        }
    }
}
