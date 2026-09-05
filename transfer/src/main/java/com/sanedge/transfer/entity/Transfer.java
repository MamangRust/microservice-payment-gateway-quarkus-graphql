package com.sanedge.transfer.entity;

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
@Table(name = "transfers")
public class Transfer extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    public Long transferId;

    @Column(name = "transfer_no", nullable = false, unique = true)
    public UUID transferNo;

    @Column(name = "transfer_from", nullable = false)
    public String transferFrom;

    @Column(name = "transfer_to", nullable = false)
    public String transferTo;

    @Column(name = "transfer_amount", nullable = false)
    public Integer transferAmount;

    @Column(name = "transfer_time", nullable = false)
    public Timestamp transferTime;

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