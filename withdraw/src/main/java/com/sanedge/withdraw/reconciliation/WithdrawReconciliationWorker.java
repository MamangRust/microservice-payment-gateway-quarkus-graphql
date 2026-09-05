package com.sanedge.withdraw.reconciliation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.withdraw.entity.Withdraw;
import com.sanedge.withdraw.repository.WithdrawCommandRepository;
import com.sanedge.withdraw.repository.WithdrawQueryRepository;

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
 * Durable reconciliation worker for withdraws. A withdraw only ever debits the
 * card balance (leg A); compensation credits the same amount back through the
 * idempotent saldo mutation ledger. The operation key is deterministic per
 * ledger record so lease steals never double-credit.
 */
@Unremovable
@ApplicationScoped
public class WithdrawReconciliationWorker {
    private static final Logger log = LoggerFactory.getLogger(WithdrawReconciliationWorker.class);
    private static final String WORKER_ID = "withdraw-reconciliation-worker";

    @Inject
    Vertx vertx;

    @Inject
    WithdrawQueryRepository withdrawQueryRepository;

    @Inject
    WithdrawCommandRepository withdrawCommandRepository;

    @Inject
    @GrpcClient("saldo")
    SaldoCommandService saldoCommandService;

    @ConfigProperty(name = "withdraw.reconciliation.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "withdraw.reconciliation.interval-ms", defaultValue = "30000")
    long intervalMs;

    @ConfigProperty(name = "withdraw.reconciliation.max-attempts", defaultValue = "5")
    int maxAttempts;

    @ConfigProperty(name = "withdraw.reconciliation.lease-minutes", defaultValue = "2")
    long leaseMinutes;

    private Long timerId;

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("WithdrawReconciliationWorker disabled by configuration");
            return;
        }
        timerId = vertx.setPeriodic(intervalMs, id -> runCycle().subscribe().with(
                count -> log.debug("WithdrawReconciliationWorker cycle completed, processed={}", count),
                err -> log.warn("WithdrawReconciliationWorker cycle failed: {}", err.getMessage())));
        log.info("WithdrawReconciliationWorker started (every {}ms, maxAttempts={}, leaseMinutes={})",
                intervalMs, maxAttempts, leaseMinutes);
    }

    @PreDestroy
    void destroy() {
        if (timerId != null) {
            vertx.cancelTimer(timerId);
            log.info("WithdrawReconciliationWorker stopped");
        }
    }

    public Uni<Integer> runCycle() {
        return withdrawQueryRepository.findPendingCompensation(maxAttempts)
                .chain(list -> processSequentially(list, 0));
    }

    private Uni<Integer> processSequentially(java.util.List<Withdraw> list, int index) {
        if (index >= list.size()) {
            return Uni.createFrom().item(index);
        }
        Withdraw withdraw = list.get(index);
        return processRecord(withdraw)
                .onItem().transform(v -> null)
                .onFailure().recoverWithItem(() -> {
                    log.warn("Withdraw {} reconciliation failed", withdraw.getWithdrawId());
                    return null;
                })
                .chain(v -> processSequentially(list, index + 1));
    }

    private Uni<Void> processRecord(Withdraw withdraw) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(leaseMinutes));
        String claimToken = UUID.randomUUID().toString();
        Long id = withdraw.getWithdrawId();

        return withdrawCommandRepository.claimCompensation(id, WORKER_ID, claimToken, now, leaseUntil, maxAttempts)
                .chain(claimed -> {
                    if (!claimed) {
                        return Uni.createFrom().voidItem();
                    }
                    boolean applied = Boolean.TRUE.equals(withdraw.getCompensationLegAApplied());
                    String card = withdraw.getCompensationLegACard();
                    Integer delta = withdraw.getCompensationLegADelta();

                    if (!applied || card == null || delta == null) {
                        // The debit never reached the balance: nothing to reverse.
                        return withdrawCommandRepository.completeCompensation(id, WORKER_ID, claimToken)
                                .map(v -> (Void) null);
                    }
                    int reverseDelta = -delta;
                    return saldoCommandService.updateSaldoBalance(
                            pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest.newBuilder()
                                    .setCardNumber(card)
                                    .setTotalBalance(0)
                                    .setDeltaBalance(reverseDelta)
                                    .setMinimumBalance(0)
                                    .setOperationKey("withdraw-comp:" + id)
                                    .build())
                            .chain(resp -> {
                                if (resp == null || !"success".equalsIgnoreCase(resp.getStatus())) {
                                    return failAndRelease(id, claimToken,
                                            resp == null ? "saldo service unavailable" : resp.getMessage());
                                }
                                return withdrawCommandRepository.completeCompensation(id, WORKER_ID, claimToken)
                                        .map(v -> (Void) null);
                            })
                            .onFailure().recoverWithUni(err -> failAndRelease(id, claimToken,
                                    "compensation adapter failed: " + err.getMessage()));
                });
    }

    private Uni<Void> failAndRelease(Long id, String claimToken, String reason) {
        return withdrawCommandRepository.releaseCompensation(id, WORKER_ID, claimToken,
                Timestamp.valueOf(LocalDateTime.now().plusSeconds(60)), reason)
                .chain(released -> withdrawCommandRepository.exhaustCompensation(id, maxAttempts, reason))
                .map(v -> (Void) null);
    }
}
