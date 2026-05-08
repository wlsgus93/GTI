package com.gametrend.insight.infrastructure.persistence.game;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameJpaRepository extends JpaRepository<GameJpaEntity, Long> {

    Optional<GameJpaEntity> findBySteamAppId(Long steamAppId);

    Optional<GameJpaEntity> findByIgdbId(Long igdbId);

    @EntityGraph(attributePaths = {"genres"})
    Page<GameJpaEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"genres"})
    Optional<GameJpaEntity> findById(Long id);
}
