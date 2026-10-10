package com.keyloop.scheduler.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "opening_hours")
public class OpeningHours {

    @Embeddable
    public static class Id implements Serializable {

        @Column(name = "dealership_id")
        private UUID dealershipId;

        // SMALLINT column: a short keeps ddl-auto=validate happy
        @Column(name = "day_of_week")
        private short dayOfWeek;

        protected Id() {
        }

        public Id(UUID dealershipId, DayOfWeek dayOfWeek) {
            this.dealershipId = dealershipId;
            this.dayOfWeek = (short) dayOfWeek.getValue();
        }

        public UUID getDealershipId() {
            return dealershipId;
        }

        public short getDayOfWeekValue() {
            return dayOfWeek;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Id other
                    && dayOfWeek == other.dayOfWeek
                    && Objects.equals(dealershipId, other.dealershipId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dealershipId, dayOfWeek);
        }
    }

    @EmbeddedId
    private Id id;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    protected OpeningHours() {
    }

    public OpeningHours(UUID dealershipId, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        this.id = new Id(dealershipId, dayOfWeek);
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public UUID getDealershipId() {
        return id.getDealershipId();
    }

    /** Stored as the ISO number 1-7; 1 = MONDAY. */
    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.of(id.getDayOfWeekValue());
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }
}
