package com.sanedge.statsreader.handler;

import com.google.protobuf.Empty;

import static com.sanedge.statsreader.handler.StatsRow.intOf;
import static com.sanedge.statsreader.handler.StatsRow.longOf;
import static com.sanedge.statsreader.handler.StatsRow.strOf;

import io.grpc.Status;
import com.sanedge.statsreader.repository.ClickHouseStatsRepository;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Singleton;
import pb.card.CardDashboard.ApiResponseDashboardCard;
import pb.card.CardDashboard.ApiResponseDashboardCardNumber;
import pb.card.CardDashboard.CardResponseDashboard;
import pb.card.CardDashboard.CardResponseDashboardCardNumber;
import pb.card.Card.FindByCardNumberRequest;
import pb.card.MutinyCardDashboardServiceGrpc;

@GrpcService
@Singleton
public class CardDashboardGrpcHandler extends MutinyCardDashboardServiceGrpc.CardDashboardServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseDashboardCard> dashboardCard(Empty request) {
        return repo.dashboard(null)
                .map(d -> ApiResponseDashboardCard.newBuilder()
                        .setStatus("success").setMessage("Retrieved card dashboard")
                        .setData(CardResponseDashboard.newBuilder()
                                .setTotalBalance(longOf(d, "total_balance"))
                                .setTotalTopup(longOf(d, "total_topup"))
                                .setTotalWithdraw(longOf(d, "total_withdraw"))
                                .setTotalTransaction(longOf(d, "total_transaction"))
                                .setTotalTransfer(longOf(d, "total_transfer")))
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseDashboardCardNumber> dashboardCardNumber(FindByCardNumberRequest request) {
        return repo.dashboard(request.getCardNumber())
                .map(d -> ApiResponseDashboardCardNumber.newBuilder()
                        .setStatus("success").setMessage("Retrieved card dashboard by card number")
                        .setData(CardResponseDashboardCardNumber.newBuilder()
                                .setTotalBalance(longOf(d, "total_balance"))
                                .setTotalTopup(longOf(d, "total_topup"))
                                .setTotalWithdraw(longOf(d, "total_withdraw"))
                                .setTotalTransaction(longOf(d, "total_transaction"))
                                .setTotalTransferSend(longOf(d, "total_transfer_send"))
                                .setTotalTransferReceiver(longOf(d, "total_transfer_receiver")))
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
