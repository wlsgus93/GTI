package com.gametrend.insight.infrastructure.persistence.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * GameJpaRepository slice 테스트 — 실제 PostgreSQL 컨테이너 (Testcontainers).
 *
 * <p>Flyway 마이그레이션이 컨테이너 시작 시 자동 실행됨.
 *
 * <p>{@code @DataJpaTest}는 기본적으로 in-memory DB로 교체하지만
 * {@code @AutoConfigureTestDatabase(replace=NONE)} + {@code @ServiceConnection}로 실제 컨테이너 사용.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class GameJpaRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GameJpaRepository gameRepository;

    @Autowired
    private GenreJpaRepository genreRepository;

    @Test
    @DisplayName("Game 저장 후 steamAppId로 조회 성공")
    void saveAndFindBySteamAppId() {
        // Arrange
        GameJpaEntity game = new GameJpaEntity();
        game.setSteamAppId(99000730L);
        game.setName("Counter-Strike 2");
        game.setDeveloper("Valve");
        game.setPublisher("Valve");
        game.setReleaseDate(LocalDate.of(2023, 9, 27));

        // Act
        GameJpaEntity saved = gameRepository.save(game);
        Optional<GameJpaEntity> found = gameRepository.findBySteamAppId(99000730L);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Counter-Strike 2");
        assertThat(found.get().getDeveloper()).isEqualTo("Valve");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("steamAppId 중복 저장 시 unique 제약 동작")
    void duplicateSteamAppIdShouldFail() {
        // Arrange
        GameJpaEntity g1 = new GameJpaEntity();
        g1.setSteamAppId(99000570L);
        g1.setName("Dota 2");
        gameRepository.saveAndFlush(g1);

        GameJpaEntity g2 = new GameJpaEntity();
        g2.setSteamAppId(99000570L); // 중복
        g2.setName("Dota 2 Duplicate");

        // Act + Assert
        assertThat(g1.getId()).isNotNull();
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> gameRepository.saveAndFlush(g2));
    }

    @Test
    @DisplayName("Game-Genre ManyToMany 저장 + 조회 (EntityGraph로 N+1 회피)")
    void gameWithGenresPersistedAndFetched() {
        // Arrange
        // V6 시드(Action/RPG/Indie/Adventure/Roguelike)와 충돌 회피 — 테스트 전용 unique 이름
        GenreJpaEntity rpg = genreRepository.save(GenreJpaEntity.of("Test-RPG"));
        GenreJpaEntity action = genreRepository.save(GenreJpaEntity.of("Test-Action"));

        GameJpaEntity game = new GameJpaEntity();
        game.setSteamAppId(99001245L);
        game.setName("ELDEN RING");
        game.setGenres(new HashSet<>(java.util.List.of(rpg, action)));
        gameRepository.save(game);

        // Act
        Optional<GameJpaEntity> loaded = gameRepository.findById(game.getId());

        // Assert
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getGenres()).hasSize(2);
        assertThat(loaded.get().getGenres())
                .extracting(GenreJpaEntity::getName)
                .containsExactlyInAnyOrder("Test-RPG", "Test-Action");
    }

    @Test
    @DisplayName("Game record 매핑 (toDomain) — 도메인 모델 변환")
    void toDomainProducesValidGameRecord() {
        // Arrange
        GameJpaEntity game = new GameJpaEntity();
        game.setSteamAppId(99000440L);
        game.setName("Team Fortress 2");
        game.setDeveloper("Valve");
        gameRepository.saveAndFlush(game);

        // Act
        var domain = gameRepository.findById(game.getId()).orElseThrow().toDomain();

        // Assert
        assertThat(domain.id()).isEqualTo(game.getId());
        assertThat(domain.steamAppId()).isEqualTo(99000440L);
        assertThat(domain.name()).isEqualTo("Team Fortress 2");
        assertThat(domain.developer()).isEqualTo("Valve");
        assertThat(domain.createdAt()).isNotNull();
    }
}
