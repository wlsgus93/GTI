package com.gametrend.insight.infrastructure.persistence.snapshot;

import com.gametrend.insight.domain.snapshot.PriceSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_snapshot")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SnapshotSource source;

    @Column(nullable = false)
    private boolean stale;

    public PriceSnapshot toDomain() {
        return new PriceSnapshot(id, gameId, currency, priceCents, discountPercent, capturedAt, source, stale);
    }

    public static PriceSnapshotJpaEntity from(PriceSnapshot s) {
        PriceSnapshotJpaEntity e = new PriceSnapshotJpaEntity();
        e.id = s.id();
        e.gameId = s.gameId();
        e.currency = s.currency();
        e.priceCents = s.priceCents();
        e.discountPercent = s.discountPercent();
        e.capturedAt = s.capturedAt();
        e.source = s.source();
        e.stale = s.stale();
        return e;
    }
}
