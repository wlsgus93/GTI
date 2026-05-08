package com.gametrend.insight.infrastructure.external.igdb;

import com.gametrend.insight.application.port.out.IgdbPort.IgdbGameMeta;
import com.gametrend.insight.infrastructure.external.igdb.dto.IgdbGameDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public final class IgdbMapper {

    private IgdbMapper() {}

    public static Optional<IgdbGameMeta> toDomain(IgdbGameDto[] response) {
        if (response == null || response.length == 0) {
            return Optional.empty();
        }
        IgdbGameDto dto = response[0];
        return Optional.of(new IgdbGameMeta(
                dto.id(),
                dto.name(),
                dto.summary(),
                toDate(dto.firstReleaseDate()),
                toGenreNames(dto.genres()),
                toCoverUrl(dto.cover())));
    }

    private static LocalDate toDate(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static List<String> toGenreNames(List<IgdbGameDto.Genre> genres) {
        if (genres == null) {
            return List.of();
        }
        return genres.stream().map(IgdbGameDto.Genre::name).toList();
    }

    private static String toCoverUrl(IgdbGameDto.Cover cover) {
        if (cover == null || cover.url() == null) {
            return null;
        }
        // IGDB는 보통 //images.igdb.com/... 같은 protocol-relative URL 반환
        String url = cover.url();
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        return url;
    }
}
