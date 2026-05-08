package com.gametrend.insight.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * IGDB 메타데이터 포트. Game 마스터 enrichment 용.
 *
 * <p>IGDB는 시계열 스냅샷이 아니라 메타데이터이므로 별도 값 객체 반환.
 */
public interface IgdbPort {

    Optional<IgdbGameMeta> fetchGameMetadata(long igdbId);

    /**
     * IGDB 게임 메타데이터 (도메인 enrichment용 값 객체).
     */
    record IgdbGameMeta(
            long igdbId,
            String name,
            String summary,
            LocalDate releaseDate,
            List<String> genres,
            String coverUrl) {}
}
