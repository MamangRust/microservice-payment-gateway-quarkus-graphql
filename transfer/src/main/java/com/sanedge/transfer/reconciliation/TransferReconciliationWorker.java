package com.sanedge.transfer.reconciliation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.transfer.entity.Transfer;
import com.sanedge.transfer.repository.TransferCommandRepository;
import com.sanedge.transfer.repository.TransferQueryRepository;

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
 * Durable reconciliation worker for transfers. A transfer has up to two
 * confirmed saldo legs: leg A debits the sender card, leg B credits the
 * receiver card. The worker reverses only the legs confirmed applied, each with
 * its own deterministic operation key, so a crash mid-saga resumes without
 * double-reversing either leg.
 */
@Unremovable
@ApplicationScoped
public class TransferReconciliationWorker {
    private static final Logger log = LoggerFactory.getLogger(TransferReconciliationWorker.class);
    private static final String WORKER_ID = "transfer-reconciliation-worker";

    @Inject
    Vertx vertx;

    @Inject
    TransferQueryRepository transferQueryRepository;

    @Inject
    TransferCommandRepository transferCommandRepository;

    @Inject
    @GrpcClient("saldo")
    SaldoCommandService saldoCommandService;

    @ConfigProperty(name = "transfer.reconciliation.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "transfer.reconciliation.interval-ms", defaultValue = "30000")
    long intervalMs;

    @ConfigProperty(name = "transfer.reconciliation.max-attempts", defaultValue = "5")
    int maxAttempts;

    @ConfigProperty(name = "transfer.reconciliation.lease-minutes", defaultValue = "2")
    long leaseMinutes;

    private Long timerId;

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("TransferReconciliationWorker disabled by configuration");
            return;
        }
        timerId = vertx.setPeriodic(intervalMs, id -> runCycle().subscribe().with(
                count -> log.debug("TransferReconciliationWorker cycle completed, processed={}", count),
                err -> log.warn("TransferReconciliationWorker cycle failed: {}", err.getMessage())));
        log.info("TransferReconciliationWorker started (every {}ms, maxAttempts={}, leaseMinutes={})",
                intervalMs, maxAttempts, leaseMinutes);
    }

    @PreDestroy
    void destroy() {
        if (timerId != null) {
            vertx.cancelTimer(timerId);
            log.info("TransferReconciliationWorker stopped");
        }
    }

    public Uni<Integer> runCycle() {
        return transferQueryRepository.findPendingCompensation(maxAttempts)
                .chain(list -> processSequentially(list, 0));
    }

    private Uni<Integer> processSequentially(java.util.List<Transfer> list, int index) {
        if (index >= list.size()) {
            return Uni.createFrom().item(index);
        }
        Transfer transfer = list.get(index);
        return processRecord(transfer)
                .onItem().transform(v -> null)
                .onFailure().recoverWithItem(() -> {
                    log.warn("Transfer {} reconciliation failed", transfer.getTransferId());
                    return null;
                })
                .chain(v -> processSequentially(list, index + 1));
    }

    private Uni<Void> processRecord(Transfer transfer) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(leaseMinutes));
        String claimToken = UUID.randomUUID().toString();
        Long id = transfer.getTransferId();

        return transferCommandRepository.claimCompensation(id, WORKER_ID, claimToken, now, leaseUntil, maxAttempts)
                .chain(claimed -> {
                    if (!claimed) {
                        return Uni.createFrom().voidItem();
                    }
                    return reverseAppliedLegs(transfer, claimToken)
                            .chain(ok -> {
                                if (!ok) {
                                    return Uni.createFrom().voidItem();
                                }
                                return transferCommandRepository.completeCompensation(id, WORKER_ID, claimToken)
                                        .map(v -> (Void) null);
                            });
                });
    }

    /**
     * Reverses every leg that was confirmed applied, sequentially, each with a
     * deterministic operation key. Returns true when every required reversal
     * succeeded (or none was required).
     */
    private Uni<Boolean> reverseAppliedLegs(Transfer transfer, String claimToken) {
        Long id = transfer.getTransferId();

        return reverseLeg(id, claimToken, "a",
                Boolean.TRUE.equals(transfer.getCompensationLegAApplied()),
                transfer.getCompensationLegACard(), transfer.getCompensationLegADelta())
                .chain(okA -> {
                    if (!okA) {
                        return Uni.createFrom().item(false);
                    }
                    return reverseLeg(id, claimToken, "b",
                            Boolean.TRUE.equals(transfer.getCompensationLegBApplied()),
                            transfer.getCompensationLegBCard(), transfer.getCompensationLegBDelta());
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
                        .setOperationKey("trf-comp:" + id + ":" + leg)
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
        return transferCommandRepository.releaseCompensation(id, WORKER_ID, claimToken,
                Timestamp.valueOf(LocalDateTime.now().plusSeconds(60)), reason)
                .chain(released -> transferCommandRepository.exhaustCompensation(id, maxAttempts, reason))
                .map(v -> (Void) null);
    }
}
