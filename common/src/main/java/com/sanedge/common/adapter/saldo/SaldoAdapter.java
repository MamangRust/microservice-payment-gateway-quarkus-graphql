package com.sanedge.common.adapter.saldo;

import com.sanedge.common.adapter.model.Saldo;
import com.sanedge.common.adapter.model.SaldoMutationResult;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.card.Card.FindByCardNumberRequest;
import pb.saldo.Saldo.SaldoResponse;
import pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest;
import pb.saldo.SaldoCommand.UpdateSaldoWithdrawRequest;
import pb.saldo.SaldoCommandService;
import pb.saldo.SaldoQueryService;

@ApplicationScoped
public class SaldoAdapter implements SaldoPort {

    @GrpcClient("saldo")
    SaldoQueryService query;

    @GrpcClient("saldo")
    SaldoCommandService command;

    @Override
    public Uni<Saldo> findByCardNumber(String cardNumber) {
        return query.findByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Saldo not found for card: " + cardNumber);
                    }
                    return toSaldo(resp.getData());
                });
    }

    @Override
    public Uni<SaldoMutationResult> updateSaldoBalance(BalanceUpdate data) {
        UpdateSaldoBalanceRequest.Builder builder = UpdateSaldoBalanceRequest.newBuilder()
                .setCardNumber(data.cardNumber())
                .setTotalBalance(data.totalBalance());
        if (data.deltaBalance() != null) {
            builder.setDeltaBalance(data.deltaBalance());
        }
        if (data.minimumBalance() != null) {
            builder.setMinimumBalance(data.minimumBalance());
        }
        if (data.withdrawAmount() != null) {
            builder.setWithdrawAmount(data.withdrawAmount());
        }
        if (data.withdrawTime() != null) {
            builder.setWithdrawTime(data.withdrawTime());
        }
        if (data.operationKey() != null) {
            builder.setOperationKey(data.operationKey());
        }
        return command.updateSaldoBalance(builder.build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Saldo not found for card: " + data.cardNumber());
                    }
                    return toMutation(resp.getData());
                });
    }

    @Override
    public Uni<SaldoMutationResult> updateSaldoWithdraw(WithdrawUpdate data) {
        UpdateSaldoWithdrawRequest.Builder builder = UpdateSaldoWithdrawRequest.newBuilder()
                .setCardNumber(data.cardNumber())
                .setTotalBalance(data.totalBalance());
        if (data.withdrawTime() != null) {
            builder.setWithdrawTime(data.withdrawTime());
        }
        if (data.withdrawAmount() != null) {
            builder.setWithdrawAmount(data.withdrawAmount());
        }
        if (data.deltaBalance() != null) {
            builder.setDeltaBalance(data.deltaBalance());
        }
        if (data.minimumBalance() != null) {
            builder.setMinimumBalance(data.minimumBalance());
        }
        if (data.operationKey() != null) {
            builder.setOperationKey(data.operationKey());
        }
        return command.updateSaldoWithdraw(builder.build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Saldo not found for card: " + data.cardNumber());
                    }
                    return toMutation(resp.getData());
                });
    }

    private static Saldo toSaldo(SaldoResponse r) {
        if (r == null) {
            return null;
        }
        Integer withdrawAmount = r.getWithdrawAmount() == 0 ? null : r.getWithdrawAmount();
        return new Saldo(r.getSaldoId(), r.getCardNumber(), r.getTotalBalance(), withdrawAmount,
                ProtoTime.parse(r.getWithdrawTime()), ProtoTime.parse(r.getCreatedAt()),
                ProtoTime.parse(r.getUpdatedAt()));
    }

    private static SaldoMutationResult toMutation(SaldoResponse r) {
        if (r == null) {
            return null;
        }
        return new SaldoMutationResult(r.getSaldoId(), r.getCardNumber(), r.getTotalBalance());
    }
}
