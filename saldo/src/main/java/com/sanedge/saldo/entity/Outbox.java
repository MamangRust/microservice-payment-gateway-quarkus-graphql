package com.sanedge.saldo.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Transactional outbox row (transaction module, schema {@code payment_finance}).
 *
 * <p>Written in the same DB transaction as the business entity it describes,
 * then relayed to Kafka by {@code OutboxPublisher}. The {@code domain} column
 * discriminates rows owned by each finance module (transaction/topup/transfer/
 * withdraw/saldo) that share this table — a publisher only claims its own
 * domain's rows.</p>
 */
@Data
@Entity
@Table(name = "outbox")
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "domain", nullable = false, length = 50)
    private String domain;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "event_key", length = 200)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private Timestamp nextAttemptAt;

    @Column(name = "claimed_at")
    private Timestamp claimedAt;

    @Column(name = "claim_token", length = 100)
    private String claimToken;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "deleted_at")
    private Timestamp deletedAt;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
