package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.TopupDto.*;
import com.sanedge.gateway.service.TopupService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TopupServiceImpl implements TopupService {

    private static final Logger LOG = Logger.getLogger(TopupServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("topup")
    pb.topup.MutinyTopupQueryServiceGrpc.MutinyTopupQueryServiceStub topupQueryService;

    @GrpcClient("topup")
    pb.topup.MutinyTopupCommandServiceGrpc.MutinyTopupCommandServiceStub topupCommandService;

    @GrpcClient("statsreader")
    pb.topup.stats.MutinyTopupStatsAmountServiceGrpc.MutinyTopupStatsAmountServiceStub topupStatsAmountService;

    @GrpcClient("statsreader")
    pb.topup.stats.MutinyTopupStatsMethodServiceGrpc.MutinyTopupStatsMethodServiceStub topupStatsMethodService;

    @GrpcClient("statsreader")
    pb.topup.stats.MutinyTopupStatsStatusServiceGrpc.MutinyTopupStatsStatusServiceStub topupStatsStatusService;

    @Override
    public Uni<FindAllTopupResponse> listTopups(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("topup.listTopups", () -> topupQueryService.findAllTopup(
                pb.topup.TopupQuery.FindAllTopupRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list topups: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTopupResponse> listTopupsByCard(String cardNumber, int page, int size, String search) {
        return telemetryHelper.traceAndMetric("topup.listTopupsByCard", () -> topupQueryService.findAllTopupByCardNumber(
                pb.topup.TopupQuery.FindAllTopupByCardNumberRequest.newBuilder()
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list topups by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTopupResponse> findActiveTopups(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("topup.findActiveTopups", () -> topupQueryService.findByActive(
                pb.topup.TopupQuery.FindAllTopupRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active topups: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllTopupResponse> findTrashedTopups(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("topup.findTrashedTopups", () -> topupQueryService.findByTrashed(
                pb.topup.TopupQuery.FindAllTopupRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed topups: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdTopupResponse> getTopup(int id) {
        return telemetryHelper.traceAndMetric("topup.getTopup", () -> topupQueryService.findByIdTopup(
                pb.topup.Topup.FindByIdTopupRequest.newBuilder()
                        .setTopupId(id)
                        .build())
                .map(FindByIdTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get topup: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdTopupResponse> getTopupByCard(String cardNumber, int year) {
        return telemetryHelper.traceAndMetric("topup.getTopupByCard", () -> topupQueryService.findByCardNumberTopup(
                pb.topup.Topup.FindByCardNumberTopupRequest.newBuilder()
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .setYear(year)
                        .build())
                .map(FindByIdTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find topup by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateTopupResponse> createTopup(CreateTopupRequest body) {
        return telemetryHelper.traceAndMetric("topup.createTopup", () -> topupCommandService.createTopup(
                pb.topup.TopupCommand.CreateTopupRequest.newBuilder()
                        .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                        .setTopupAmount(body.topupAmount())
                        .setTopupMethod(body.topupMethod() == null ? "" : body.topupMethod())
                        .build())
                .map(CreateTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create topup: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateTopupResponse> updateTopup(int id, UpdateTopupRequest body) {
        return telemetryHelper.traceAndMetric("topup.updateTopup", () -> topupCommandService.updateTopup(
                pb.topup.TopupCommand.UpdateTopupRequest.newBuilder()
                        .setTopupId(id)
                        .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                        .setTopupAmount(body.topupAmount())
                        .setTopupMethod(body.topupMethod() == null ? "" : body.topupMethod())
                        .build())
                .map(UpdateTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update topup: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteTopupPermanent(int id) {
        return telemetryHelper.traceAndMetric("topup.deleteTopupPermanent", () -> topupCommandService.deleteTopupPermanent(
                pb.topup.Topup.FindByIdTopupRequest.newBuilder()
                        .setTopupId(id)
                        .build())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete topup: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedTopupResponse> trashTopup(int id) {
        return telemetryHelper.traceAndMetric("topup.trashTopup", () -> topupCommandService.trashedTopup(
                pb.topup.Topup.FindByIdTopupRequest.newBuilder()
                        .setTopupId(id)
                        .build())
                .map(TrashedTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash topup: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedTopupResponse> restoreTopup(int id) {
        return telemetryHelper.traceAndMetric("topup.restoreTopup", () -> topupCommandService.restoreTopup(
                pb.topup.Topup.FindByIdTopupRequest.newBuilder()
                        .setTopupId(id)
                        .build())
                .map(TrashedTopupResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore topup: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> restoreAllTopups() {
        return telemetryHelper.traceAndMetric("topup.restoreAllTopups", () -> topupCommandService.restoreAllTopup(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all topups: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteAllTopups() {
        return telemetryHelper.traceAndMetric("topup.deleteAllTopups", () -> topupCommandService.deleteAllTopupPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all topups: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupMonthAmount> findMonthlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("topup.findMonthlyAmounts", () -> topupStatsAmountService.findMonthlyTopupAmounts(
                pb.topup.Topup.FindYearTopupStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTopupMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly topup amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupYearAmount> findYearlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("topup.findYearlyAmounts", () -> topupStatsAmountService.findYearlyTopupAmounts(
                pb.topup.Topup.FindYearTopupStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTopupYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly topup amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupMonthAmount> findMonthlyAmountsByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("topup.findMonthlyAmountsByCard", () -> topupStatsAmountService.findMonthlyTopupAmountsByCardNumber(
                pb.topup.Topup.FindYearTopupCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTopupMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly topup amounts by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupYearAmount> findYearlyAmountsByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("topup.findYearlyAmountsByCard", () -> topupStatsAmountService.findYearlyTopupAmountsByCardNumber(
                pb.topup.Topup.FindYearTopupCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTopupYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly topup amounts by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupMonthMethod> findMonthlyMethods(int year, int month) {
        return telemetryHelper.traceAndMetric("topup.findMonthlyMethods", () -> topupStatsMethodService.findMonthlyTopupMethods(
                pb.topup.Topup.FindYearTopupStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTopupMonthMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly methods: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupYearMethod> findYearlyMethods(int year) {
        return telemetryHelper.traceAndMetric("topup.findYearlyMethods", () -> topupStatsMethodService.findYearlyTopupMethods(
                pb.topup.Topup.FindYearTopupStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTopupYearMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly methods: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupMonthMethod> findMonthlyMethodsByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("topup.findMonthlyMethodsByCard", () -> topupStatsMethodService.findMonthlyTopupMethodsByCardNumber(
                pb.topup.Topup.FindYearTopupCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTopupMonthMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly methods by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupYearMethod> findYearlyMethodsByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("topup.findYearlyMethodsByCard", () -> topupStatsMethodService.findYearlyTopupMethodsByCardNumber(
                pb.topup.Topup.FindYearTopupCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTopupYearMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly methods by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupMonthStatusSuccess> findMonthlyStatusSuccess(int year, int month) {
        return telemetryHelper.traceAndMetric("topup.findMonthlyStatusSuccess", () -> topupStatsStatusService.findMonthlyTopupStatusSuccess(
                pb.topup.Topup.FindMonthlyTopupStatus.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(ApiResponseTopupMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupYearStatusSuccess> findYearlyStatusSuccess(int year) {
        return telemetryHelper.traceAndMetric("topup.findYearlyStatusSuccess", () -> topupStatsStatusService.findYearlyTopupStatusSuccess(
                pb.topup.Topup.FindYearTopupStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTopupYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupMonthStatusFailed> findMonthlyStatusFailed(int year, int month) {
        return telemetryHelper.traceAndMetric("topup.findMonthlyStatusFailed", () -> topupStatsStatusService.findMonthlyTopupStatusFailed(
                pb.topup.Topup.FindMonthlyTopupStatus.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(ApiResponseTopupMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupYearStatusFailed> findYearlyStatusFailed(int year) {
        return telemetryHelper.traceAndMetric("topup.findYearlyStatusFailed", () -> topupStatsStatusService.findYearlyTopupStatusFailed(
                pb.topup.Topup.FindYearTopupStatus.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseTopupYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupMonthStatusSuccess> findMonthlyStatusSuccessByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("topup.findMonthlyStatusSuccessByCard", () -> topupStatsStatusService.findMonthlyTopupStatusSuccessByCardNumber(
                pb.topup.Topup.FindMonthlyTopupStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTopupMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status success by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupYearStatusSuccess> findYearlyStatusSuccessByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("topup.findYearlyStatusSuccessByCard", () -> topupStatsStatusService.findYearlyTopupStatusSuccessByCardNumber(
                pb.topup.Topup.FindYearTopupStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTopupYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status success by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupMonthStatusFailed> findMonthlyStatusFailedByCard(int year, int month, String cardNumber) {
        return telemetryHelper.traceAndMetric("topup.findMonthlyStatusFailedByCard", () -> topupStatsStatusService.findMonthlyTopupStatusFailedByCardNumber(
                pb.topup.Topup.FindMonthlyTopupStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTopupMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find monthly status failed by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseTopupYearStatusFailed> findYearlyStatusFailedByCard(int year, String cardNumber) {
        return telemetryHelper.traceAndMetric("topup.findYearlyStatusFailedByCard", () -> topupStatsStatusService.findYearlyTopupStatusFailedByCardNumber(
                pb.topup.Topup.FindYearTopupStatusCardNumber.newBuilder()
                        .setYear(year)
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(ApiResponseTopupYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find yearly status failed by card: " + throwable.getMessage(), throwable)));
    }
}
