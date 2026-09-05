package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.CardDto.*;
import com.sanedge.gateway.service.CardService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CardServiceImpl implements CardService {

    private static final Logger LOG = Logger.getLogger(CardServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("card")
    pb.card.MutinyCardQueryServiceGrpc.MutinyCardQueryServiceStub cardQueryService;

    @GrpcClient("card")
    pb.card.MutinyCardCommandServiceGrpc.MutinyCardCommandServiceStub cardCommandService;

    @GrpcClient("statsreader")
    pb.card.MutinyCardDashboardServiceGrpc.MutinyCardDashboardServiceStub cardDashboardService;

    @GrpcClient("statsreader")
    pb.card.stats.MutinyCardStatsBalanceServiceGrpc.MutinyCardStatsBalanceServiceStub cardStatsBalanceService;

    @GrpcClient("statsreader")
    pb.card.stats.MutinyCardStatsTopupServiceGrpc.MutinyCardStatsTopupServiceStub cardStatsTopupService;

    @GrpcClient("statsreader")
    pb.card.stats.MutinyCardStatsTransactionServiceGrpc.MutinyCardStatsTransactionServiceStub cardStatsTransactionService;

    @GrpcClient("statsreader")
    pb.card.stats.MutinyCardStatsTransferServiceGrpc.MutinyCardStatsTransferServiceStub cardStatsTransferService;

    @GrpcClient("statsreader")
    pb.card.stats.MutinyCardStatsWithdrawServiceGrpc.MutinyCardStatsWithdrawServiceStub cardStatsWithdrawService;

    private com.google.protobuf.Timestamp parseTimestamp(String dateStr) {
        try {
            java.time.Instant instant = java.time.Instant.parse(dateStr);
            return com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
        } catch (Exception e) {
            return com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(System.currentTimeMillis() / 1000)
                    .build();
        }
    }

    @Override
    public Uni<FindAllCardResponse> listCards(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("card.listCards", () -> cardQueryService.findAllCard(pb.card.Card.FindAllCardRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(FindAllCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list cards: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllCardResponse> findActiveCards(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("card.findActiveCards", () -> cardQueryService.findByActiveCard(pb.card.Card.FindAllCardRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(FindAllCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active cards: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllCardResponse> findTrashedCards(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("card.findTrashedCards", () -> cardQueryService.findByTrashedCard(pb.card.Card.FindAllCardRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(FindAllCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed cards: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdCardResponse> getCard(int id) {
        return telemetryHelper.traceAndMetric("card.getCard", () -> cardQueryService.findByIdCard(pb.card.Card.FindByIdCardRequest.newBuilder()
                .setCardId(id)
                .build())
                .map(FindByIdCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdCardResponse> findCardByUser(int userId) {
        return telemetryHelper.traceAndMetric("card.findCardByUser", () -> cardQueryService.findByUserIdCard(pb.card.Card.FindByUserIdCardRequest.newBuilder()
                .setUserId(userId)
                .build())
                .map(FindByIdCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find card by user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdCardResponse> findCardByNumber(String cardNumber) {
        return telemetryHelper.traceAndMetric("card.findCardByNumber", () -> cardQueryService.findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(FindByIdCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find card by number: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CardWithEmailResponse> findUserCardByCardNumber(String cardNumber) {
        return telemetryHelper.traceAndMetric("card.findUserCardByCardNumber", () -> cardQueryService.findUserCardByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(CardWithEmailResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find user card by number: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateCardResponse> createCard(CreateCardRequest body) {
        return telemetryHelper.traceAndMetric("card.createCard", () -> cardCommandService.createCard(pb.card.CardCommand.CreateCardRequest.newBuilder()
                .setUserId(body.userId())
                .setCardType(body.cardType() == null ? "" : body.cardType())
                .setExpireDate(parseTimestamp(body.expireDate()))
                .setCvv(body.cvv() == null ? "" : body.cvv())
                .setCardProvider(body.cardProvider() == null ? "" : body.cardProvider())
                .build())
                .map(CreateCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateCardResponse> updateCard(int id, UpdateCardRequest body) {
        return telemetryHelper.traceAndMetric("card.updateCard", () -> cardCommandService.updateCard(pb.card.CardCommand.UpdateCardRequest.newBuilder()
                .setCardId(id)
                .setUserId(body.userId())
                .setCardType(body.cardType() == null ? "" : body.cardType())
                .setExpireDate(parseTimestamp(body.expireDate()))
                .setCvv(body.cvv() == null ? "" : body.cvv())
                .setCardProvider(body.cardProvider() == null ? "" : body.cardProvider())
                .build())
                .map(UpdateCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteCardPermanent(int id) {
        return telemetryHelper.traceAndMetric("card.deleteCardPermanent", () -> cardCommandService.deleteCardPermanent(pb.card.Card.FindByIdCardRequest.newBuilder()
                .setCardId(id)
                .build())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedCardResponse> deleteCard(int id) {
        return telemetryHelper.traceAndMetric("card.deleteCard", () -> cardCommandService.trashedCard(pb.card.Card.FindByIdCardRequest.newBuilder()
                .setCardId(id)
                .build())
                .map(TrashedCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedCardResponse> trashCard(int id) {
        return telemetryHelper.traceAndMetric("card.trashCard", () -> cardCommandService.trashedCard(pb.card.Card.FindByIdCardRequest.newBuilder()
                .setCardId(id)
                .build())
                .map(TrashedCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedCardResponse> restoreCard(int id) {
        return telemetryHelper.traceAndMetric("card.restoreCard", () -> cardCommandService.restoreCard(pb.card.Card.FindByIdCardRequest.newBuilder()
                .setCardId(id)
                .build())
                .map(TrashedCardResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> restoreAllCards() {
        return telemetryHelper.traceAndMetric("card.restoreAllCards", () -> cardCommandService.restoreAllCard(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all cards: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteAllCards() {
        return telemetryHelper.traceAndMetric("card.deleteAllCards", () -> cardCommandService.deleteAllCardPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all cards: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyBalance> findMonthlyBalance(int year) {
        return telemetryHelper.traceAndMetric("card.findMonthlyBalance", () -> cardStatsBalanceService.findMonthlyBalance(pb.card.stats.CardStatsBalance.FindYearBalance.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseMonthlyBalance::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly balance: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyBalance> findYearlyBalance(int year) {
        return telemetryHelper.traceAndMetric("card.findYearlyBalance", () -> cardStatsBalanceService.findYearlyBalance(pb.card.stats.CardStatsBalance.FindYearBalance.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseYearlyBalance::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly balance: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyBalance> getMonthlyBalanceByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getMonthlyBalanceByCard", () -> cardStatsBalanceService.findMonthlyBalanceByCardNumber(pb.card.stats.CardStatsBalance.FindYearBalanceCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseMonthlyBalance::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly balance by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyBalance> getYearlyBalanceByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getYearlyBalanceByCard", () -> cardStatsBalanceService.findYearlyBalanceByCardNumber(pb.card.stats.CardStatsBalance.FindYearBalanceCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseYearlyBalance::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly balance by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyTopupAmount(int year) {
        return telemetryHelper.traceAndMetric("card.findMonthlyTopupAmount", () -> cardStatsTopupService.findMonthlyTopupAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly topup amount: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> findYearlyTopupAmount(int year) {
        return telemetryHelper.traceAndMetric("card.findYearlyTopupAmount", () -> cardStatsTopupService.findYearlyTopupAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly topup amount: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> getMonthlyTopupAmountByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getMonthlyTopupAmountByCard", () -> cardStatsTopupService.findMonthlyTopupAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly topup by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> getYearlyTopupAmountByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getYearlyTopupAmountByCard", () -> cardStatsTopupService.findYearlyTopupAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly topup by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransactionAmount(int year) {
        return telemetryHelper.traceAndMetric("card.findMonthlyTransactionAmount", () -> cardStatsTransactionService.findMonthlyTransactionAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transaction: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> findYearlyTransactionAmount(int year) {
        return telemetryHelper.traceAndMetric("card.findYearlyTransactionAmount", () -> cardStatsTransactionService.findYearlyTransactionAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transaction: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> getMonthlyTransactionAmountByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getMonthlyTransactionAmountByCard", () -> cardStatsTransactionService.findMonthlyTransactionAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly transaction by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> getYearlyTransactionAmountByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getYearlyTransactionAmountByCard", () -> cardStatsTransactionService.findYearlyTransactionAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly transaction by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransferAmountSender(int year) {
        return telemetryHelper.traceAndMetric("card.findMonthlyTransferAmountSender", () -> cardStatsTransferService.findMonthlyTransferSenderAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transfer sender: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransferAmountReceiver(int year) {
        return telemetryHelper.traceAndMetric("card.findMonthlyTransferAmountReceiver", () -> cardStatsTransferService.findMonthlyTransferReceiverAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transfer receiver: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> findYearlyTransferAmountSender(int year) {
        return telemetryHelper.traceAndMetric("card.findYearlyTransferAmountSender", () -> cardStatsTransferService.findYearlyTransferSenderAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transfer sender: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> findYearlyTransferAmountReceiver(int year) {
        return telemetryHelper.traceAndMetric("card.findYearlyTransferAmountReceiver", () -> cardStatsTransferService.findYearlyTransferReceiverAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transfer receiver: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> getMonthlyTransferAmountByCardSender(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getMonthlyTransferAmountByCardSender", () -> cardStatsTransferService.findMonthlyTransferSenderAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly transfer sender by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> getMonthlyTransferAmountByCardReceiver(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getMonthlyTransferAmountByCardReceiver", () -> cardStatsTransferService.findMonthlyTransferReceiverAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly transfer receiver by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> getYearlyTransferAmountByCardSender(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getYearlyTransferAmountByCardSender", () -> cardStatsTransferService.findYearlyTransferSenderAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly transfer sender by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> getYearlyTransferAmountByCardReceiver(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getYearlyTransferAmountByCardReceiver", () -> cardStatsTransferService.findYearlyTransferReceiverAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly transfer receiver by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyWithdrawAmount(int year) {
        return telemetryHelper.traceAndMetric("card.findMonthlyWithdrawAmount", () -> cardStatsWithdrawService.findMonthlyWithdrawAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly withdraw amount: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> findYearlyWithdrawAmount(int year) {
        return telemetryHelper.traceAndMetric("card.findYearlyWithdrawAmount", () -> cardStatsWithdrawService.findYearlyWithdrawAmount(pb.card.Card.FindYearAmount.newBuilder()
                .setYear(year)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly withdraw amount: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> getMonthlyWithdrawAmountByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getMonthlyWithdrawAmountByCard", () -> cardStatsWithdrawService.findMonthlyWithdrawAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly withdraw by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearlyAmount> getYearlyWithdrawAmountByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("card.getYearlyWithdrawAmountByCard", () -> cardStatsWithdrawService.findYearlyWithdrawAmountByCardNumber(pb.card.Card.FindYearAmountCardNumber.newBuilder()
                .setYear(year)
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly withdraw by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseDashboardCard> findCardDashboard() {
        return telemetryHelper.traceAndMetric("card.findCardDashboard", () -> cardDashboardService.dashboardCard(com.google.protobuf.Empty.getDefaultInstance())
                .map(ApiResponseDashboardCard::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get card dashboard: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseDashboardCardNumber> findCardDashboardByCardNumber(String cardNumber) {
        return telemetryHelper.traceAndMetric("card.findCardDashboardByCardNumber", () -> cardDashboardService.dashboardCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                .setCardNumber(cardNumber == null ? "" : cardNumber)
                .build())
                .map(ApiResponseDashboardCardNumber::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get card dashboard by card number: " + throwable.getMessage(), throwable)));
    }
}
