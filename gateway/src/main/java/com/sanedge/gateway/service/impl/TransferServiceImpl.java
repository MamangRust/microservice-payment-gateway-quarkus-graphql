package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.TransferDto.*;
import com.sanedge.gateway.service.TransferService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TransferServiceImpl implements TransferService {

    private static final Logger LOG = Logger.getLogger(TransferServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("transfer")
    pb.transfer.MutinyTransferQueryServiceGrpc.MutinyTransferQueryServiceStub transferQueryService;

    @GrpcClient("transfer")
    pb.transfer.MutinyTransferCommandServiceGrpc.MutinyTransferCommandServiceStub transferCommandService;

    @GrpcClient("statsreader")
    pb.transfer.stats.MutinyTransferStatsAmountServiceGrpc.MutinyTransferStatsAmountServiceStub transferStatsAmountService;

    @GrpcClient("statsreader")
    pb.transfer.stats.MutinyTransferStatsStatusServiceGrpc.MutinyTransferStatsStatusServiceStub transferStatsStatusService;

    @Override
    public Uni<FindAllTransferResponse> listTransfers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transfer.listTransfers", () -> transferQueryService.findAllTransfer(
                pb.transfer.Transfer.FindAllTransferRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list transfers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTransferResponse> findActiveTransfers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transfer.findActiveTransfers", () -> transferQueryService.findByActiveTransfer(
                pb.transfer.Transfer.FindAllTransferRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active transfers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTransferResponse> findTrashedTransfers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transfer.findTrashedTransfers", () -> transferQueryService.findByTrashedTransfer(
                pb.transfer.Transfer.FindAllTransferRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed transfers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdTransferResponse> getTransfer(int id) {
        return telemetryHelper.traceAndMetric("transfer.getTransfer", () -> transferQueryService.findByIdTransfer(
                pb.transfer.Transfer.FindByIdTransferRequest.newBuilder()
                        .setTransferId(id)
                        .build())
                .map(FindByIdTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get transfer: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTransferResponse> findTransfersFrom(String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findTransfersFrom", () -> transferQueryService.findTransferByTransferFrom(
                pb.transfer.Transfer.FindTransferByTransferFromRequest.newBuilder()
                        .setTransferFrom(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(FindAllTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find transfers from: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTransferResponse> findTransfersTo(String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findTransfersTo", () -> transferQueryService.findTransferByTransferTo(
                pb.transfer.Transfer.FindTransferByTransferToRequest.newBuilder()
                        .setTransferTo(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(FindAllTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find transfers to: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateTransferResponse> createTransfer(CreateTransferRequest body) {
        return telemetryHelper.traceAndMetric("transfer.createTransfer", () -> transferCommandService.createTransfer(
                pb.transfer.TransferCommand.CreateTransferRequest.newBuilder()
                        .setTransferFrom(body.transferFrom() == null ? "" : body.transferFrom())
                        .setTransferTo(body.transferTo() == null ? "" : body.transferTo())
                        .setTransferAmount(body.transferAmount())
                        .build())
                .map(CreateTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create transfer: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateTransferResponse> updateTransfer(int id, UpdateTransferRequest body) {
        return telemetryHelper.traceAndMetric("transfer.updateTransfer", () -> transferCommandService.updateTransfer(
                pb.transfer.TransferCommand.UpdateTransferRequest.newBuilder()
                        .setTransferId(id)
                        .setTransferFrom(body.transferFrom() == null ? "" : body.transferFrom())
                        .setTransferTo(body.transferTo() == null ? "" : body.transferTo())
                        .setTransferAmount(body.transferAmount())
                        .build())
                .map(UpdateTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update transfer: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteTransferPermanent(int id) {
        return telemetryHelper.traceAndMetric("transfer.deleteTransferPermanent", () -> transferCommandService.deleteTransferPermanent(
                pb.transfer.Transfer.FindByIdTransferRequest.newBuilder()
                        .setTransferId(id)
                        .build())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete transfer: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedTransferResponse> trashTransfer(int id) {
        return telemetryHelper.traceAndMetric("transfer.trashTransfer", () -> transferCommandService.trashedTransfer(
                pb.transfer.Transfer.FindByIdTransferRequest.newBuilder()
                        .setTransferId(id)
                        .build())
                .map(TrashedTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash transfer: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedTransferResponse> restoreTransfer(int id) {
        return telemetryHelper.traceAndMetric("transfer.restoreTransfer", () -> transferCommandService.restoreTransfer(
                pb.transfer.Transfer.FindByIdTransferRequest.newBuilder()
                        .setTransferId(id)
                        .build())
                .map(TrashedTransferResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore transfer: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> restoreAllTransfers() {
        return telemetryHelper.traceAndMetric("transfer.restoreAllTransfers", () -> transferCommandService.restoreAllTransfer(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all transfers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteAllTransfers() {
        return telemetryHelper.traceAndMetric("transfer.deleteAllTransfers", () -> transferCommandService.deleteAllTransferPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all transfers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferMonthAmount> findMonthlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("transfer.findMonthlyAmounts", () -> transferStatsAmountService.findMonthlyTransferAmounts(
                pb.transfer.Transfer.FindYearTransferStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransferMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transfer amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferYearAmount> findYearlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("transfer.findYearlyAmounts", () -> transferStatsAmountService.findYearlyTransferAmounts(
                pb.transfer.Transfer.FindYearTransferStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransferYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transfer amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferMonthAmount> findMonthlyAmountsFromCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findMonthlyAmountsFromCard", () -> transferStatsAmountService.findMonthlyTransferAmountsBySenderCardNumber(
                pb.transfer.Transfer.FindByCardNumberTransferRequest.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransferMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transfer amounts from card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferMonthAmount> findMonthlyAmountsToCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findMonthlyAmountsToCard", () -> transferStatsAmountService.findMonthlyTransferAmountsByReceiverCardNumber(
                pb.transfer.Transfer.FindByCardNumberTransferRequest.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransferMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly transfer amounts to card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferYearAmount> findYearlyAmountsFromCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findYearlyAmountsFromCard", () -> transferStatsAmountService.findYearlyTransferAmountsBySenderCardNumber(
                pb.transfer.Transfer.FindByCardNumberTransferRequest.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransferYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transfer amounts from card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferYearAmount> findYearlyAmountsToCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findYearlyAmountsToCard", () -> transferStatsAmountService.findYearlyTransferAmountsByReceiverCardNumber(
                pb.transfer.Transfer.FindByCardNumberTransferRequest.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransferYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly transfer amounts to card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferMonthStatusSuccess> findMonthlyStatusSuccess(int year, int month) {
        return telemetryHelper.traceAndMetric("transfer.findMonthlyStatusSuccess", () -> transferStatsStatusService.findMonthlyTransferStatusSuccess(
                pb.transfer.Transfer.FindMonthlyTransferStatus.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(ApiResponseTransferMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferYearStatusSuccess> findYearlyStatusSuccess(int year) {
        return telemetryHelper.traceAndMetric("transfer.findYearlyStatusSuccess", () -> transferStatsStatusService.findYearlyTransferStatusSuccess(
                pb.transfer.Transfer.FindYearTransferStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransferYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferMonthStatusFailed> findMonthlyStatusFailed(int year, int month) {
        return telemetryHelper.traceAndMetric("transfer.findMonthlyStatusFailed", () -> transferStatsStatusService.findMonthlyTransferStatusFailed(
                pb.transfer.Transfer.FindMonthlyTransferStatus.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(ApiResponseTransferMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferYearStatusFailed> findYearlyStatusFailed(int year) {
        return telemetryHelper.traceAndMetric("transfer.findYearlyStatusFailed", () -> transferStatsStatusService.findYearlyTransferStatusFailed(
                pb.transfer.Transfer.FindYearTransferStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTransferYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferMonthStatusSuccess> findMonthlyStatusSuccessByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findMonthlyStatusSuccessByCard", () -> transferStatsStatusService.findMonthlyTransferStatusSuccessByCardNumber(
                pb.transfer.Transfer.FindMonthlyTransferStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransferMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status success by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferYearStatusSuccess> findYearlyStatusSuccessByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findYearlyStatusSuccessByCard", () -> transferStatsStatusService.findYearlyTransferStatusSuccessByCardNumber(
                pb.transfer.Transfer.FindYearTransferStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransferYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status success by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferMonthStatusFailed> findMonthlyStatusFailedByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findMonthlyStatusFailedByCard", () -> transferStatsStatusService.findMonthlyTransferStatusFailedByCardNumber(
                pb.transfer.Transfer.FindMonthlyTransferStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransferMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status failed by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTransferYearStatusFailed> findYearlyStatusFailedByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("transfer.findYearlyStatusFailedByCard", () -> transferStatsStatusService.findYearlyTransferStatusFailedByCardNumber(
                pb.transfer.Transfer.FindYearTransferStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTransferYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status failed by card: " + throwable.getMessage(), throwable)));
    }
}
