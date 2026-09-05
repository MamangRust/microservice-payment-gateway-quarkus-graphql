package com.sanedge.topup.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.enums.Status;
import com.sanedge.topup.entity.Topup;
import com.sanedge.topup.repository.TopupCommandRepository;
import com.sanedge.topup.repository.TopupQueryRepository;

import io.smallrye.mutiny.Uni;
import pb.saldo.SaldoCommandService;

@ExtendWith(MockitoExtension.class)
class TopupReconciliationWorkerTest {

    @Mock
    TopupQueryRepository topupQueryRepository;

    @Mock
    TopupCommandRepository topupCommandRepository;

    @Mock
    SaldoCommandService saldoCommandService;

    private TopupReconciliationWorker worker;

    @BeforeEach
    void setUp() throws Exception {
        worker = new TopupReconciliationWorker();
        setField("topupQueryRepository", topupQueryRepository);
        setField("topupCommandRepository", topupCommandRepository);
        setField("saldoCommandService", saldoCommandService);
        // config fields are only read by init()/claims; set sensible values
        setField("enabled", false);
        setField("intervalMs", 30000L);
        setField("maxAttempts", 5);
        setField("leaseMinutes", 2L);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = TopupReconciliationWorker.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(worker, value);
    }

    private Topup pendingTopup(boolean applied, int delta) {
        Topup t = new Topup();
        t.setTopupId(1L);
        t.setTopupNo(UUID.randomUUID());
        t.setCardNumber("1234-5678-9012-3456");
        t.setTopupAmount(delta);
        t.setStatus(Status.COMPENSATION_REQUIRED);
        t.setCompensationAttempts(0);
        t.setCompensationLegACard("1234-5678-9012-3456");
        t.setCompensationLegADelta(delta);
        t.setCompensationLegAApplied(applied);
        t.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        t.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return t;
    }

    private pb.saldo.Saldo.ApiResponseSaldo saldoResp(String status) {
        return pb.saldo.Saldo.ApiResponseSaldo.newBuilder().setStatus(status).build();
    }

    @Test
    void appliesReverseDeltaWithDeterministicKeyAndCompletes() {
        when(topupQueryRepository.findPendingCompensation(5))
                .thenReturn(Uni.createFrom().item(Collections.singletonList(pendingTopup(true, 50000))));
        when(topupCommandRepository.claimCompensation(any(Long.class), anyString(), anyString(),
                any(Timestamp.class), any(Timestamp.class), any(Integer.class)))
                .thenReturn(Uni.createFrom().item(true));
        when(saldoCommandService.updateSaldoBalance(any()))
                .thenReturn(Uni.createFrom().item(saldoResp("success")));
        when(topupCommandRepository.completeCompensation(any(Long.class), anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(true));

        int processed = worker.runCycle().await().indefinitely();

        assertThat(processed).isEqualTo(1);
        ArgumentCaptor<pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest> captor = ArgumentCaptor
                .forClass(pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest.class);
        verify(saldoCommandService).updateSaldoBalance(captor.capture());
        assertThat(captor.getValue().getDeltaBalance()).isEqualTo(-50000);
        assertThat(captor.getValue().getOperationKey()).isEqualTo("topup-comp:1");
        verify(topupCommandRepository).completeCompensation(eq(1L), eq("topup-reconciliation-worker"), anyString());
    }

    @Test
    void releasesAndExhaustsWhenAdapterFails() {
        when(topupQueryRepository.findPendingCompensation(5))
                .thenReturn(Uni.createFrom().item(Collections.singletonList(pendingTopup(true, 50000))));
        when(topupCommandRepository.claimCompensation(any(Long.class), anyString(), anyString(),
                any(Timestamp.class), any(Timestamp.class), any(Integer.class)))
                .thenReturn(Uni.createFrom().item(true));
        when(saldoCommandService.updateSaldoBalance(any()))
                .thenReturn(Uni.createFrom().item(saldoResp("error")));
        when(topupCommandRepository.releaseCompensation(any(Long.class), anyString(), anyString(),
                any(Timestamp.class), anyString()))
                .thenReturn(Uni.createFrom().item(true));
        when(topupCommandRepository.exhaustCompensation(any(Long.class), any(Integer.class), anyString()))
                .thenReturn(Uni.createFrom().item(true));

        int processed = worker.runCycle().await().indefinitely();

        assertThat(processed).isEqualTo(1);
        verify(topupCommandRepository).releaseCompensation(any(Long.class), anyString(), anyString(),
                any(Timestamp.class), anyString());
        verify(topupCommandRepository).exhaustCompensation(any(Long.class), any(Integer.class), anyString());
        verify(topupCommandRepository, never()).completeCompensation(any(Long.class), anyString(), anyString());
    }

    @Test
    void completesWithoutReversalWhenNoLegWasApplied() {
        when(topupQueryRepository.findPendingCompensation(5))
                .thenReturn(Uni.createFrom().item(Collections.singletonList(pendingTopup(false, 50000))));
        when(topupCommandRepository.claimCompensation(any(Long.class), anyString(), anyString(),
                any(Timestamp.class), any(Timestamp.class), any(Integer.class)))
                .thenReturn(Uni.createFrom().item(true));
        when(topupCommandRepository.completeCompensation(any(Long.class), anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(true));

        int processed = worker.runCycle().await().indefinitely();

        assertThat(processed).isEqualTo(1);
        verify(saldoCommandService, never()).updateSaldoBalance(any());
        verify(topupCommandRepository).completeCompensation(eq(1L), eq("topup-reconciliation-worker"), anyString());
    }

    @Test
    void skipsRecordsClaimedByAnotherWorker() {
        when(topupQueryRepository.findPendingCompensation(5))
                .thenReturn(Uni.createFrom().item(Collections.singletonList(pendingTopup(true, 50000))));
        when(topupCommandRepository.claimCompensation(any(Long.class), anyString(), anyString(),
                any(Timestamp.class), any(Timestamp.class), any(Integer.class)))
                .thenReturn(Uni.createFrom().item(false));

        int processed = worker.runCycle().await().indefinitely();

        assertThat(processed).isEqualTo(1);
        verify(saldoCommandService, never()).updateSaldoBalance(any());
        verify(topupCommandRepository, never()).completeCompensation(any(Long.class), anyString(), anyString());
    }
}
