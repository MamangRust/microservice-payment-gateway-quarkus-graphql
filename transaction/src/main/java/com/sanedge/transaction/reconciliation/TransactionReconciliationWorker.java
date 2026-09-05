package com.sanedge.transaction.reconciliation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.repository.TransactionCommandRepository;
import com.sanedge.transaction.repository.TransactionQueryRepository;

import io.quarkus.arc.Unremovable;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pb.saldo.SaldoCommandService;

/**
 * Durable reconciliation worker for transactions. A transaction has up to two
 * confirmed saldo legs: leg A debits the paying card, leg B credits the
 * merchant card. The worker reverses only the legs the ledger confirms were
 * applied (compensation_leg_*_applied), each with its own deterministic
 * operation key, so a crash between legs is resumed without double-reversing
 * either leg.
 */
@Unremovable
@ApplicationScoped
public class TransactionReconciliationWorker {
    private static final Logger log = LoggerFactory.getLogger(TransactionReconciliationWorker.class);
    private static final String WORKER_ID = "transaction-reconciliation-worker";

    @Inject
    Vertx vertx;

    @Inject
    TransactionQueryRepository transactionQueryRepository;

    @Inject
    TransactionCommandRepository transactionCommandRepository;

    @Inject
    @GrpcClient("saldo")
    SaldoCommandService saldoCommandService;

    @ConfigProperty(name = "transaction.reconciliation.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "transaction.reconciliation.interval-ms", defaultValue = "30000")
    long intervalMs;

    @ConfigProperty(name = "transaction.reconciliation.max-attempts", defaultValue = "5")
    int maxAttempts;

    @ConfigProperty(name = "transaction.reconciliation.lease-minutes", defaultValue = "2")
    long leaseMinutes;

    private Long timerId;

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("TransactionReconciliationWorker disabled by configuration");
            return;
        }
        timerId = vertx.setPeriodic(intervalMs, id -> runCycle().subscribe().with(
                count -> log.debug("TransactionReconciliationWorker cycle completed, processed={}", count),
                err -> log.warn("TransactionReconciliationWorker cycle failed: {}", err.getMessage())));
        log.info("TransactionReconciliationWorker started (every {}ms, maxAttempts={}, leaseMinutes={})",
                intervalMs, maxAttempts, leaseMinutes);
    }

    @PreDestroy
    void destroy() {
        if (timerId != null) {
            vertx.cancelTimer(timerId);
            log.info("TransactionReconciliationWorker stopped");
        }
    }

    public Uni<Integer> runCycle() {
        return transactionQueryRepository.findPendingCompensation(maxAttempts)
                .chain(list -> processSequentially(list, 0));
    }

    private Uni<Integer> processSequentially(java.util.List<Transaction> list, int index) {
        if (index >= list.size()) {
            return Uni.createFrom().item(index);
        }
        Transaction transaction = list.get(index);
        return processRecord(transaction)
                .onItem().transform(v -> null)
                .onFailure().recoverWithItem(() -> {
                    log.warn("Transaction {} reconciliation failed", transaction.getTransactionId());
                    return null;
                })
                .chain(v -> processSequentially(list, index + 1));
    }

    private Uni<Void> processRecord(Transaction transaction) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(leaseMinutes));
        String claimToken = UUID.randomUUID().toString();
        Long id = transaction.getTransactionId();

        return transactionCommandRepository.claimCompensation(id, WORKER_ID, claimToken, now, leaseUntil, maxAttempts)
                .chain(claimed -> {
                    if (!claimed) {
                        return Uni.createFrom().voidItem();
                    }
                    return reverseAppliedLegs(transaction, claimToken)
                            .chain(ok -> {
                                if (!ok) {
                                    return Uni.createFrom().voidItem();
                                }
                                return transactionCommandRepository.completeCompensation(id, WORKER_ID, claimToken)
                                        .map(v -> (Void) null);
                            });
                });
    }

    /**
     * Reverses every leg that was confirmed applied, sequentially, each with a
     * deterministic operation key. Returns true when every required reversal
     * succeeded (or none was required).
     */
    private Uni<Boolean> reverseAppliedLegs(Transaction transaction, String claimToken) {
        Long id = transaction.getTransactionId();

        return reverseLeg(id, claimToken, "a",
                Boolean.TRUE.equals(transaction.getCompensationLegAApplied()),
                transaction.getCompensationLegACard(), transaction.getCompensationLegADelta())
                .chain(okA -> {
                    if (!okA) {
                        return Uni.createFrom().item(false);
                    }
                    return reverseLeg(id, claimToken, "b",
                            Boolean.TRUE.equals(transaction.getCompensationLegBApplied()),
                            transaction.getCompensationLegBCard(), transaction.getCompensationLegBDelta());
                });
    }

    private Uni<Boolean> reverseLeg(Long id, String claimToken, String leg, boolean applied,
            String card, Integer delta) {
        if (!applied || card == null || delta == null) {
            return Uni.createFrom().item(true);
        }
        int reverseDelta = -delta;
        return saldoCommandService.updateSaldoBalance(
                pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest.newBuilder()
                        .setCardNumber(card)
                        .setTotalBalance(0)
                        .setDeltaBalance(reverseDelta)
                        .setMinimumBalance(0)
                        .setOperationKey("txn-comp:" + id + ":" + leg)
                        .build())
                .chain(resp -> {
                    if (resp == null || !"success".equalsIgnoreCase(resp.getStatus())) {
                        return failAndRelease(id, claimToken,
                                resp == null ? "saldo service unavailable" : resp.getMessage())
                                .map(v -> false);
                    }
                    return Uni.createFrom().item(true);
                })
                .onFailure().recoverWithUni(err -> failAndRelease(id, claimToken,
                        "compensation adapter failed: " + err.getMessage()).map(v -> false));
    }

    private Uni<Void> failAndRelease(Long id, String claimToken, String reason) {
        return transactionCommandRepository.releaseCompensation(id, WORKER_ID, claimToken,
                Timestamp.valueOf(LocalDateTime.now().plusSeconds(60)), reason)
                .chain(released -> transactionCommandRepository.exhaustCompensation(id, maxAttempts, reason))
                .map(v -> (Void) null);
    }
}
