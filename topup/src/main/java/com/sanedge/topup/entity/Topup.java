package com.sanedge.topup.entity;

import java.sql.Timestamp;
import java.util.UUID;

import com.sanedge.common.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "topups")
public class Topup extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topup_id")
    public Long topupId;

    @Column(name = "topup_no", nullable = false, unique = true)
    public UUID topupNo;

    @Column(name = "card_number", nullable = false)
    public String cardNumber;

    @Column(name = "topup_amount", nullable = false)
    public Integer topupAmount;

    @Column(name = "topup_method", nullable = false)
    public String topupMethod;

    @Column(name = "topup_time", nullable = false)
    public Timestamp topupTime;

    @Column(name = "idempotency_key", length = 64, unique = true)
    public String idempotencyKey;

    @Column(name = "request_fingerprint", length = 64)
    public String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Status status = Status.PENDING;

    @Column(name = "compensation_required_at")
    public Timestamp compensationRequiredAt;

    @Column(name = "compensation_attempts", nullable = false)
    public Integer compensationAttempts = 0;

    @Column(name = "last_failure_reason", length = 500)
    public String lastFailureReason;

    @Column(name = "compensation_claimed_at")
    public Timestamp compensationClaimedAt;

    @Column(name = "compensation_claimed_by", length = 100)
    public String compensationClaimedBy;

    @Column(name = "compensation_claim_token", length = 100)
    public String compensationClaimToken;

    @Column(name = "compensation_lease_until")
    public Timestamp compensationLeaseUntil;

    @Column(name = "compensation_next_attempt_at")
    public Timestamp compensationNextAttemptAt;

    @Column(name = "compensation_leg_a_card", length = 32)
    public String compensationLegACard;
    @Column(name = "compensation_leg_a_delta")
    public Integer compensationLegADelta;
    @Column(name = "compensation_leg_a_applied", nullable = false)
    public Boolean compensationLegAApplied = false;
    @Column(name = "compensation_leg_b_card", length = 32)
    public String compensationLegBCard;
    @Column(name = "compensation_leg_b_delta")
    public Integer compensationLegBDelta;
    @Column(name = "compensation_leg_b_applied", nullable = false)
    public Boolean compensationLegBApplied = false;
}
