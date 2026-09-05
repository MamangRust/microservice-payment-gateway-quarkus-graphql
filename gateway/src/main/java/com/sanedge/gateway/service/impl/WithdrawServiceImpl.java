package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.WithdrawDto.*;
import com.sanedge.gateway.service.WithdrawService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WithdrawServiceImpl implements WithdrawService {

    private static final Logger LOG = Logger.getLogger(WithdrawServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("withdraw")
    pb.withdraw.MutinyWithdrawQueryServiceGrpc.MutinyWithdrawQueryServiceStub withdrawQueryService;

    @GrpcClient("withdraw")
    pb.withdraw.MutinyWithdrawCommandServiceGrpc.MutinyWithdrawCommandServiceStub withdrawCommandService;

    @GrpcClient("statsreader")
    pb.withdraw.stats.MutinyWithdrawStatsAmountServiceGrpc.MutinyWithdrawStatsAmountServiceStub withdrawStatsAmountService;

    @GrpcClient("statsreader")
    pb.withdraw.stats.MutinyWithdrawStatsStatusServiceGrpc.MutinyWithdrawStatsStatusServiceStub withdrawStatsStatusService;

    @Override
    public Uni<FindAllWithdrawResponse> listWithdraws(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("withdraw.listWithdraws", () -> withdrawQueryService.findAllWithdraw(
                pb.withdraw.Withdraw.FindAllWithdrawRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list withdraws: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllWithdrawResponse> findByCard(String cardNumber) {
        return telemetryHelper.traceAndMetric("withdraw.findByCard", () -> withdrawQueryService.findByCardNumber(
                pb.card.Card.FindByCardNumberRequest.newBuilder()
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(FindAllWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find withdraws by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllWithdrawResponse> findActiveWithdraws(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("withdraw.findActiveWithdraws", () -> withdrawQueryService.findByActive(
                pb.withdraw.Withdraw.FindAllWithdrawRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active withdraws: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllWithdrawResponse> findTrashedWithdraws(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("withdraw.findTrashedWithdraws", () -> withdrawQueryService.findByTrashed(
                pb.withdraw.Withdraw.FindAllWithdrawRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed withdraws: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdWithdrawResponse> getWithdraw(int id) {
        return telemetryHelper.traceAndMetric("withdraw.getWithdraw", () -> withdrawQueryService.findByIdWithdraw(
                pb.withdraw.Withdraw.FindByIdWithdrawRequest.newBuilder()
                        .setWithdrawId(id)
                        .build())
                .map(FindByIdWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get withdraw: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateWithdrawResponse> createWithdraw(CreateWithdrawBody body) {
        pb.withdraw.WithdrawCommand.CreateWithdrawRequest req = pb.withdraw.WithdrawCommand.CreateWithdrawRequest.newBuilder()
                .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                .setWithdrawAmount(body.withdrawAmount())
                .setWithdrawTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(System.currentTimeMillis() / 1000)
                        .build())
                .build();
        return telemetryHelper.traceAndMetric("withdraw.createWithdraw", () -> withdrawCommandService.createWithdraw(req)
                .map(CreateWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create withdraw: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateWithdrawResponse> updateWithdraw(int id, CreateWithdrawBody body) {
        pb.withdraw.WithdrawCommand.UpdateWithdrawRequest req = pb.withdraw.WithdrawCommand.UpdateWithdrawRequest.newBuilder()
                .setWithdrawId(id)
                .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                .setWithdrawAmount(body.withdrawAmount())
                .setWithdrawTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(System.currentTimeMillis() / 1000)
                        .build())
                .build();
        return telemetryHelper.traceAndMetric("withdraw.updateWithdraw", () -> withdrawCommandService.updateWithdraw(req)
                .map(UpdateWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update withdraw: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedWithdrawResponse> deleteWithdraw(int id) {
        return telemetryHelper.traceAndMetric("withdraw.deleteWithdraw", () -> withdrawCommandService.trashedWithdraw(
                pb.withdraw.Withdraw.FindByIdWithdrawRequest.newBuilder()
                        .setWithdrawId(id)
                        .build())
                .map(TrashedWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to delete withdraw: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteWithdrawPermanent(int id) {
        return telemetryHelper.traceAndMetric("withdraw.deleteWithdrawPermanent", () -> withdrawCommandService.deleteWithdrawPermanent(
                pb.withdraw.Withdraw.FindByIdWithdrawRequest.newBuilder()
                        .setWithdrawId(id)
                        .build())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete withdraw: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedWithdrawResponse> trashWithdraw(int id) {
        return telemetryHelper.traceAndMetric("withdraw.trashWithdraw", () -> withdrawCommandService.trashedWithdraw(
                pb.withdraw.Withdraw.FindByIdWithdrawRequest.newBuilder()
                        .setWithdrawId(id)
                        .build())
                .map(TrashedWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash withdraw: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedWithdrawResponse> restoreWithdraw(int id) {
        return telemetryHelper.traceAndMetric("withdraw.restoreWithdraw", () -> withdrawCommandService.restoreWithdraw(
                pb.withdraw.Withdraw.FindByIdWithdrawRequest.newBuilder()
                        .setWithdrawId(id)
                        .build())
                .map(TrashedWithdrawResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore withdraw: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> restoreAllWithdraws() {
        return telemetryHelper.traceAndMetric("withdraw.restoreAllWithdraws", () -> withdrawCommandService.restoreAllWithdraw(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all withdraws: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteAllWithdraws() {
        return telemetryHelper.traceAndMetric("withdraw.deleteAllWithdraws", () -> withdrawCommandService.deleteAllWithdrawPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all withdraws: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawMonthAmount> findMonthlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("withdraw.findMonthlyAmounts", () -> withdrawStatsAmountService.findMonthlyWithdraws(
                pb.withdraw.Withdraw.FindYearWithdrawStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseWithdrawMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly withdraw amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawYearAmount> findYearlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("withdraw.findYearlyAmounts", () -> withdrawStatsAmountService.findYearlyWithdraws(
                pb.withdraw.Withdraw.FindYearWithdrawStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseWithdrawYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly withdraw amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawMonthAmount> findMonthlyByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("withdraw.findMonthlyByCard", () -> withdrawStatsAmountService.findMonthlyWithdrawsByCardNumber(
                pb.withdraw.Withdraw.FindYearWithdrawCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseWithdrawMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly withdraws by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawYearAmount> findYearlyByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("withdraw.findYearlyByCard", () -> withdrawStatsAmountService.findYearlyWithdrawsByCardNumber(
                pb.withdraw.Withdraw.FindYearWithdrawCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseWithdrawYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly withdraws by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawMonthStatusSuccess> findMonthlyStatusSuccess(int year, int month) {
        return telemetryHelper.traceAndMetric("withdraw.findMonthlyStatusSuccess", () -> withdrawStatsStatusService.findMonthlyWithdrawStatusSuccess(
                pb.withdraw.Withdraw.FindMonthlyWithdrawStatus.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(ApiResponseWithdrawMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly withdraw status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawYearStatusSuccess> findYearlyStatusSuccess(int year) {
        return telemetryHelper.traceAndMetric("withdraw.findYearlyStatusSuccess", () -> withdrawStatsStatusService.findYearlyWithdrawStatusSuccess(
                pb.withdraw.Withdraw.FindYearWithdrawStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseWithdrawYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly withdraw status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawMonthStatusFailed> findMonthlyStatusFailed(int year, int month) {
        return telemetryHelper.traceAndMetric("withdraw.findMonthlyStatusFailed", () -> withdrawStatsStatusService.findMonthlyWithdrawStatusFailed(
                pb.withdraw.Withdraw.FindMonthlyWithdrawStatus.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(ApiResponseWithdrawMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly withdraw status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawYearStatusFailed> findYearlyStatusFailed(int year) {
        return telemetryHelper.traceAndMetric("withdraw.findYearlyStatusFailed", () -> withdrawStatsStatusService.findYearlyWithdrawStatusFailed(
                pb.withdraw.Withdraw.FindYearWithdrawStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseWithdrawYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly withdraw status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawMonthStatusSuccess> findMonthlyStatusSuccessByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("withdraw.findMonthlyStatusSuccessByCard", () -> withdrawStatsStatusService.findMonthlyWithdrawStatusSuccessCardNumber(
                pb.withdraw.Withdraw.FindMonthlyWithdrawStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseWithdrawMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly withdraw status success by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawYearStatusSuccess> findYearlyStatusSuccessByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("withdraw.findYearlyStatusSuccessByCard", () -> withdrawStatsStatusService.findYearlyWithdrawStatusSuccessCardNumber(
                pb.withdraw.Withdraw.FindYearWithdrawStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseWithdrawYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly withdraw status success by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawMonthStatusFailed> findMonthlyStatusFailedByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("withdraw.findMonthlyStatusFailedByCard", () -> withdrawStatsStatusService.findMonthlyWithdrawStatusFailedCardNumber(
                pb.withdraw.Withdraw.FindMonthlyWithdrawStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseWithdrawMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly withdraw status failed by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseWithdrawYearStatusFailed> findYearlyStatusFailedByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("withdraw.findYearlyStatusFailedByCard", () -> withdrawStatsStatusService.findYearlyWithdrawStatusFailedCardNumber(
                pb.withdraw.Withdraw.FindYearWithdrawStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseWithdrawYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly withdraw status failed by card: " + throwable.getMessage(), throwable)));
    }
}
