package com.sanedge.card.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Durable event audit sink (Phase 4): one row per consumed card domain event
 * that was previously publish-only. Redelivery is deduplicated by the partial
 * unique index (topic, reference_id) created in migration V22; the repository
 * also performs a find-before-insert as the first line of defense.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "card_event_logs")
public class CardEventLog extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    public Long eventId;

    @Column(name = "topic", nullable = false, length = 80)
    public String topic;

    @Column(name = "event_type", nullable = false, length = 80)
    public String eventType;

    @Column(name = "card_number", length = 16)
    public String cardNumber;

    @Column(name = "reference_id", length = 64)
    public String referenceId;

    @Column(name = "payload", nullable = false, length = 10000)
    public String payload;
}
