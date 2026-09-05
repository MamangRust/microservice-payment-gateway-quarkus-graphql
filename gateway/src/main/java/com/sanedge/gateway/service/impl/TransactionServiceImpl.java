package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.TransactionDto.*;
import com.sanedge.gateway.service.TransactionService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TransactionServiceImpl implements TransactionService {

    private static final Logger LOG = Logger.getLogger(TransactionServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("transaction")
    pb.transaction.MutinyTransactionQueryServiceGrpc.MutinyTransactionQueryServiceStub transactionQueryService;

    @GrpcClient("transaction")
    pb.transaction.MutinyTransactionCommandServiceGrpc.MutinyTransactionCommandServiceStub transactionCommandService;

    @GrpcClient("statsreader")
    pb.transaction.stats.MutinyTransactionStatsAmountServiceGrpc.MutinyTransactionStatsAmountServiceStub transactionStatsAmountService;

    @GrpcClient("statsreader")
    pb.transaction.stats.MutinyTransactionStatsMethodServiceGrpc.MutinyTransactionStatsMethodServiceStub transactionStatsMethodService;

    @GrpcClient("statsreader")
    pb.transaction.stats.MutinyTransactionStatsStatusServiceGrpc.MutinyTransactionStatsStatusServiceStub transactionStatsStatusService;

    @Override
    public Uni<FindAllTransactionResponse> listTransactions(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transaction.listTransactions", () -> transactionQueryService.findAllTransaction(
                pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTransactionResponse> findTransactionsByCard(String cardNumber) {
        return telemetryHelper.traceAndMetric("transaction.findTransactionsByCard", () -> transactionQueryService.findAllTransactionByCardNumber(
                pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest.newBuilder()
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .setPage(1)
                        .setPageSize(100)
                        .setSearch("")
                        .build())
                .map(FindAllTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find transactions by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTransactionResponse> findTransactionsByMerchant(int merchantId) {
        return telemetryHelper.traceAndMetric("transaction.findTransactionsByMerchant", () -> transactionQueryService.findTransactionByMerchantId(
                pb.transaction.TransactionQuery.FindTransactionByMerchantIdRequest.newBuilder()
                        .setMerchantId(merchantId)
                        .build())
                .map(FindAllTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find transactions by merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTransactionResponse> findTransactionsByCardAndYear(String cardNumber, int year) {
        return telemetryHelper.traceAndMetric("transaction.findTransactionsByCardAndYear", () -> transactionQueryService.findAllTransactionByCardNumber(
                pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest.newBuilder()
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .setPage(1)
                        .setPageSize(1000)
                        .setSearch("")
                        .build())
                .map(apiResp -> {
                    List<TransactionResponse> filtered = apiResp.getDataList().stream()
                            .map(TransactionResponse::from)
                            .filter(t -> t.transactionTime() != null && t.transactionTime().startsWith(String.valueOf(year)))
                            .toList();
                    return new FindAllTransactionResponse(filtered, apiResp.getStatus(), apiResp.getMessage());
                })
                .onFailure().invoke(throwable -> LOG.error("Failed to find transactions by card and year: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTransactionResponse> findActiveTransactions(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transaction.findActiveTransactions", () -> transactionQueryService.findByActiveTransaction(
                pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTransactionResponse> findTrashedTransactions(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transaction.findTrashedTransactions", () -> transactionQueryService.findByTrashedTransaction(
                pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdTransactionResponse> getTransaction(int id) {
        return telemetryHelper.traceAndMetric("transaction.getTransaction", () -> transactionQueryService.findByIdTransaction(
                pb.transaction.Transaction.FindByIdTransactionRequest.newBuilder()
                        .setTransactionId(id)
                        .build())
                .map(FindByIdTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get transaction: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateTransactionResponse> createTransaction(CreateTransactionBody body) {
        return telemetryHelper.traceAndMetric("transaction.createTransaction", () -> transactionCommandService.createTransaction(
                pb.transaction.TransactionCommand.CreateTransactionRequest.newBuilder()
                        .setApiKey("")
                        .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                        .setAmount(body.amount())
                        .setPaymentMethod(body.paymentMethod() == null ? "" : body.paymentMethod())
                        .setMerchantId(body.merchantId())
                        .setTransactionTime(com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() / 1000)
                                .build())
                        .build())
                .map(CreateTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create transaction: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateTransactionResponse> updateTransaction(int id, UpdateTransactionBody body) {
        return telemetryHelper.traceAndMetric("transaction.updateTransaction", () -> transactionCommandService.updateTransaction(
                pb.transaction.TransactionCommand.UpdateTransactionRequest.newBuilder()
                        .setTransactionId(id)
                        .setApiKey("")
                        .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                        .setAmount(body.amount())
                        .setPaymentMethod(body.paymentMethod() == null ? "" : body.paymentMethod())
                        .setMerchantId(body.merchantId())
                        .setTransactionTime(com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() / 1000)
                                .build())
                        .build())
                .map(UpdateTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update transaction: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteTransactionPermanent(int id) {
        return telemetryHelper.traceAndMetric("transaction.deleteTransactionPermanent", () -> transactionCommandService.deleteTransactionPermanent(
                pb.transaction.Transaction.FindByIdTransactionRequest.newBuilder()
                        .setTransactionId(id)
                        .build())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete transaction: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedTransactionResponse> trashTransaction(int id) {
        return telemetryHelper.traceAndMetric("transaction.trashTransaction", () -> transactionCommandService.trashedTransaction(
                pb.transaction.Transaction.FindByIdTransactionRequest.newBuilder()
                        .setTransactionId(id)
                        .build())
                .map(TrashedTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash transaction: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedTransactionResponse> restoreTransaction(int id) {
        return telemetryHelper.traceAndMetric("transaction.restoreTransaction", () -> transactionCommandService.restoreTransaction(
                pb.transaction.Transaction.FindByIdTransactionRequest.newBuilder()
                        .setTransactionId(id)
                        .build())
                .map(TrashedTransactionResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore transaction: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> restoreAllTransactions() {
        return telemetryHelper.traceAndMetric("transaction.restoreAllTransactions", () -> transactionCommandService.restoreAllTransaction(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteAllTransactions() {
        return telemetryHelper.traceAndMetric("transaction.deleteAllTransactions", () -> transactionCommandService.deleteAllTransactionPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionMonthAmount> findMonthlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("transaction.findMonthlyAmounts", () -> transactionStatsAmountService.findMonthlyAmounts(
                pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransactionMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transaction amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionYearAmount> findYearlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("transaction.findYearlyAmounts", () -> transactionStatsAmountService.findYearlyAmounts(
                pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransactionYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transaction amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionMonthAmount> findMonthlyAmountsByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transaction.findMonthlyAmountsByCard", () -> transactionStatsAmountService.findMonthlyAmountsByCardNumber(
                pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransactionMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transaction amounts by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionYearAmount> findYearlyAmountsByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transaction.findYearlyAmountsByCard", () -> transactionStatsAmountService.findYearlyAmountsByCardNumber(
                pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransactionYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transaction amounts by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionMonthMethod> findMonthlyMethods(int year, int month) {
        return telemetryHelper.traceAndMetric("transaction.findMonthlyMethods", () -> transactionStatsMethodService.findMonthlyPaymentMethods(
                pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransactionMonthMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transaction methods: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionYearMethod> findYearlyMethods(int year) {
        return telemetryHelper.traceAndMetric("transaction.findYearlyMethods", () -> transactionStatsMethodService.findYearlyPaymentMethods(
                pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransactionYearMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transaction methods: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionMonthMethod> findMonthlyMethodsByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("transaction.findMonthlyMethodsByCard", () -> transactionStatsMethodService.findMonthlyPaymentMethodsByCardNumber(
                pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransactionMonthMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transaction methods by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionYearMethod> findYearlyMethodsByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transaction.findYearlyMethodsByCard", () -> transactionStatsMethodService.findYearlyPaymentMethodsByCardNumber(
                pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransactionYearMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transaction methods by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionMonthStatusSuccess> findMonthlyStatusSuccess(int year, int month) {
        return telemetryHelper.traceAndMetric("transaction.findMonthlyStatusSuccess", () -> transactionStatsStatusService.findMonthlyTransactionStatusSuccess(
                pb.transaction.Transaction.FindMonthlyTransactionStatus.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(ApiResponseTransactionMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionYearStatusSuccess> findYearlyStatusSuccess(int year) {
        return telemetryHelper.traceAndMetric("transaction.findYearlyStatusSuccess", () -> transactionStatsStatusService.findYearlyTransactionStatusSuccess(
                pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransactionYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionMonthStatusFailed> findMonthlyStatusFailed(int year, int month) {
        return telemetryHelper.traceAndMetric("transaction.findMonthlyStatusFailed", () -> transactionStatsStatusService.findMonthlyTransactionStatusFailed(
                pb.transaction.Transaction.FindMonthlyTransactionStatus.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(ApiResponseTransactionMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionYearStatusFailed> findYearlyStatusFailed(int year) {
        return telemetryHelper.traceAndMetric("transaction.findYearlyStatusFailed", () -> transactionStatsStatusService.findYearlyTransactionStatusFailed(
                pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransactionYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionMonthStatusSuccess> findMonthlyStatusSuccessByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("transaction.findMonthlyStatusSuccessByCard", () -> transactionStatsStatusService.findMonthlyTransactionStatusSuccessByCardNumber(
                pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransactionMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status success by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionYearStatusSuccess> findYearlyStatusSuccessByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transaction.findYearlyStatusSuccessByCard", () -> transactionStatsStatusService.findYearlyTransactionStatusSuccessByCardNumber(
                pb.transaction.Transaction.FindYearTransactionStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransactionYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status success by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionMonthStatusFailed> findMonthlyStatusFailedByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("transaction.findMonthlyStatusFailedByCard", () -> transactionStatsStatusService.findMonthlyTransactionStatusFailedByCardNumber(
                pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransactionMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status failed by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransactionYearStatusFailed> findYearlyStatusFailedByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transaction.findYearlyStatusFailedByCard", () -> transactionStatsStatusService.findYearlyTransactionStatusFailedByCardNumber(
                pb.transaction.Transaction.FindYearTransactionStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransactionYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status failed by card: " + throwable.getMessage(), throwable)));
    }
}
