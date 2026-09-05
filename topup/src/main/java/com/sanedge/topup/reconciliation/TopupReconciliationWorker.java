package com.sanedge.topup.reconciliation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.topup.entity.Topup;
import com.sanedge.topup.repository.TopupCommandRepository;
import com.sanedge.topup.repository.TopupQueryRepository;

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
 * Durable reconciliation worker for topups. Claims compensation records with an
 * expiring lease (fencing), reverses the single confirmed saldo leg through the
 * idempotent saldo mutation ledger, and either completes the record
 * (COMPENSATED) or backs off and, after exhausting retries, fails it
 * terminally. The compensation operation key is deterministic per ledger
 * record, so retries after a lease steal never reverse the same credit twice.
 */
@Unremovable
@ApplicationScoped
public class TopupReconciliationWorker {
    private static final Logger log = LoggerFactory.getLogger(TopupReconciliationWorker.class);
    private static final String WORKER_ID = "topup-reconciliation-worker";

    @Inject
    Vertx vertx;

    @Inject
    TopupQueryRepository topupQueryRepository;

    @Inject
    TopupCommandRepository topupCommandRepository;

    @Inject
    @GrpcClient("saldo")
    SaldoCommandService saldoCommandService;

    @ConfigProperty(name = "topup.reconciliation.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "topup.reconciliation.interval-ms", defaultValue = "30000")
    long intervalMs;

    @ConfigProperty(name = "topup.reconciliation.max-attempts", defaultValue = "5")
    int maxAttempts;

    @ConfigProperty(name = "topup.reconciliation.lease-minutes", defaultValue = "2")
    long leaseMinutes;

    private Long timerId;

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("TopupReconciliationWorker disabled by configuration");
            return;
        }
        timerId = vertx.setPeriodic(intervalMs, id -> runCycle().subscribe().with(
                count -> log.debug("TopupReconciliationWorker cycle completed, processed={}", count),
                err -> log.warn("TopupReconciliationWorker cycle failed: {}", err.getMessage())));
        log.info("TopupReconciliationWorker started (every {}ms, maxAttempts={}, leaseMinutes={})",
                intervalMs, maxAttempts, leaseMinutes);
    }

    @PreDestroy
    void destroy() {
        if (timerId != null) {
            vertx.cancelTimer(timerId);
            log.info("TopupReconciliationWorker stopped");
        }
    }

    public Uni<Integer> runCycle() {
        return topupQueryRepository.findPendingCompensation(maxAttempts)
                .chain(list -> processSequentially(list, 0));
    }

    private Uni<Integer> processSequentially(java.util.List<Topup> list, int index) {
        if (index >= list.size()) {
            return Uni.createFrom().item(index);
        }
        Topup topup = list.get(index);
        return processRecord(topup)
                .onItem().transform(v -> null)
                .onFailure().recoverWithItem(() -> {
                    log.warn("Topup {} reconciliation failed", topup.getTopupId());
                    return null;
                })
                .chain(v -> processSequentially(list, index + 1));
    }

    private Uni<Void> processRecord(Topup topup) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(leaseMinutes));
        String claimToken = UUID.randomUUID().toString();
        Long id = topup.getTopupId();

        return topupCommandRepository.claimCompensation(id, WORKER_ID, claimToken, now, leaseUntil, maxAttempts)
                .chain(claimed -> {
                    if (!claimed) {
                        return Uni.createFrom().voidItem();
                    }
                    boolean applied = Boolean.TRUE.equals(topup.getCompensationLegAApplied());
                    String card = topup.getCompensationLegACard();
                    Integer delta = topup.getCompensationLegADelta();

                    if (!applied || card == null || delta == null) {
                        // Nothing was applied to any balance: nothing to reverse.
                        return topupCommandRepository.completeCompensation(id, WORKER_ID, claimToken)
                                .map(v -> (Void) null);
                    }
                    int reverseDelta = -delta;
                    return saldoCommandService.updateSaldoBalance(
                            pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest.newBuilder()
                                    .setCardNumber(card)
                                    .setTotalBalance(0)
                                    .setDeltaBalance(reverseDelta)
                                    .setMinimumBalance(0)
                                    .setOperationKey("topup-comp:" + id)
                                    .build())
                            .chain(resp -> {
                                if (resp == null || !"success".equalsIgnoreCase(resp.getStatus())) {
                                    return failAndRelease(id, claimToken,
                                            resp == null ? "saldo service unavailable" : resp.getMessage());
                                }
                                return topupCommandRepository.completeCompensation(id, WORKER_ID, claimToken)
                                        .map(v -> (Void) null);
                            })
                            .onFailure().recoverWithUni(err -> failAndRelease(id, claimToken,
                                    "compensation adapter failed: " + err.getMessage()));
                });
    }

    private Uni<Void> failAndRelease(Long id, String claimToken, String reason) {
        return topupCommandRepository.releaseCompensation(id, WORKER_ID, claimToken,
                Timestamp.valueOf(LocalDateTime.now().plusSeconds(60)), reason)
                .chain(released -> topupCommandRepository.exhaustCompensation(id, maxAttempts, reason))
                .map(v -> (Void) null);
    }
}
