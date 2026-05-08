package com.gametrend.insight.application.ingestion;

import com.gametrend.insight.domain.snapshot.SnapshotSource;
import java.time.Duration;

/**
 * 한 외부 소스의 수집 결과 — 성공 또는 실패. Sealed 타입으로 패턴 매칭 강제.
 *
 * <p>오케스트레이터는 9개 소스 결과를 {@code List<IngestionResult<?>>}로 반환하며,
 * 한 소스 실패가 다른 소스를 차단하지 않음 (부분 장애 허용).
 *
 * @param <T> 소스가 반환하는 도메인 객체 (PlayerSnapshot, PriceSnapshot, ViewerSnapshot 등)
 */
public sealed interface IngestionResult<T> {

    SnapshotSource source();

    Duration duration();

    /** 라이브 호출 성공. */
    record Success<T>(SnapshotSource source, T value, Duration duration) implements IngestionResult<T> {}

    /** 호출 실패 (외부 API 에러, timeout, 응답 파싱 실패 등). */
    record Failure<T>(SnapshotSource source, Throwable error, Duration duration) implements IngestionResult<T> {}

    /** 호출 성공이지만 응답에 데이터가 없음 (예: result != 1, 게임 없음). */
    record Empty<T>(SnapshotSource source, Duration duration) implements IngestionResult<T> {}

    static <T> Success<T> success(SnapshotSource source, T value, Duration duration) {
        return new Success<>(source, value, duration);
    }

    static <T> Failure<T> failure(SnapshotSource source, Throwable error, Duration duration) {
        return new Failure<>(source, error, duration);
    }

    static <T> Empty<T> empty(SnapshotSource source, Duration duration) {
        return new Empty<>(source, duration);
    }
}
