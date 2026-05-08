package com.gametrend.insight.infrastructure.persistence.game;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreJpaRepository extends JpaRepository<GenreJpaEntity, Long> {

    Optional<GenreJpaEntity> findByName(String name);
}
