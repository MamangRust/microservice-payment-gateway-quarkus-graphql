package com.sanedge.saldo.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "saldo_mutation_operations")
public class SaldoMutationOperation {
    @Id
    @Column(name = "operation_key", length = 160)
    private String operationKey;

    @Column(name = "card_number", nullable = false, length = 32)
    private String cardNumber;

    @Column(name = "requested_delta", nullable = false)
    private Integer requestedDelta;

    @Column(name = "minimum_balance", nullable = false)
    private Integer minimumBalance = 0;

    @Column(name = "result_status", nullable = false, length = 16)
    private String resultStatus;

    @Column(name = "result_balance")
    private Integer resultBalance;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;
}
