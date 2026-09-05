package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.MerchantDto.*;
import com.sanedge.gateway.service.MerchantService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantServiceImpl implements MerchantService {

    private static final Logger LOG = Logger.getLogger(MerchantServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant")
    pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub merchantQueryService;

    @GrpcClient("merchant")
    pb.merchant.MutinyMerchantCommandServiceGrpc.MutinyMerchantCommandServiceStub merchantCommandService;

    @GrpcClient("merchant")
    pb.merchant.MutinyMerchantTransactionServiceGrpc.MutinyMerchantTransactionServiceStub merchantTransactionService;

    @GrpcClient("statsreader")
    pb.merchant.stats.MutinyMerchantStatsAmountServiceGrpc.MutinyMerchantStatsAmountServiceStub merchantStatsAmountService;

    @GrpcClient("statsreader")
    pb.merchant.stats.MutinyMerchantStatsMethodServiceGrpc.MutinyMerchantStatsMethodServiceStub merchantStatsMethodService;

    @GrpcClient("statsreader")
    pb.merchant.stats.MutinyMerchantStatsTotalAmountServiceGrpc.MutinyMerchantStatsTotalAmountServiceStub merchantStatsTotalAmountService;

    @Override
    public Uni<FindAllMerchantResponse> listMerchants(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.listMerchants", () -> merchantQueryService.findAllMerchant(
                pb.merchant.Merchant.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdMerchantResponse> getMerchant(int id) {
        return telemetryHelper.traceAndMetric("merchant.getMerchant", () -> merchantQueryService.findByIdMerchant(
                pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                        .setMerchantId(id)
                        .build())
                .map(FindByIdMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdMerchantResponse> getMerchantByApiKey(String apiKey) {
        return telemetryHelper.traceAndMetric("merchant.getMerchantByApiKey", () -> merchantQueryService.findByApiKey(
                pb.merchant.Merchant.FindByApiKeyRequest.newBuilder()
                        .setApiKey(apiKey == null ? "" : apiKey)
                        .build())
                .map(FindByIdMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get merchant by api key: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllMerchantResponse> getMerchantsByUserId(int userId) {
        return telemetryHelper.traceAndMetric("merchant.getMerchantsByUserId", () -> merchantQueryService.findByMerchantUserId(
                pb.merchant.Merchant.FindByMerchantUserIdRequest.newBuilder()
                        .setUserId(userId)
                        .build())
                .map(FindAllMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get merchants by user id: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllMerchantResponse> findActiveMerchants(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.findActiveMerchants", () -> merchantQueryService.findByActive(
                pb.merchant.Merchant.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllMerchantResponse> findTrashedMerchants(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchant.findTrashedMerchants", () -> merchantQueryService.findByTrashed(
                pb.merchant.Merchant.FindAllMerchantRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateMerchantResponse> createMerchant(CreateMerchantRequest body) {
        return telemetryHelper.traceAndMetric("merchant.createMerchant", () -> merchantCommandService.createMerchant(
                pb.merchant.MerchantCommand.CreateMerchantRequest.newBuilder()
                        .setUserId(body.userId())
                        .setName(body.name() == null ? "" : body.name())
                        .build())
                .map(CreateMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateMerchantResponse> updateMerchant(int id, UpdateMerchantRequest body) {
        return telemetryHelper.traceAndMetric("merchant.updateMerchant", () -> merchantCommandService.updateMerchant(
                pb.merchant.MerchantCommand.UpdateMerchantRequest.newBuilder()
                        .setMerchantId(id)
                        .setUserId(body.userId())
                        .setName(body.name() == null ? "" : body.name())
                        .setStatus(body.status() == null ? "" : body.status())
                        .build())
                .map(UpdateMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateMerchantResponse> updateMerchantStatus(int id, String status) {
        return telemetryHelper.traceAndMetric("merchant.updateMerchantStatus", () -> merchantCommandService.updateMerchantStatus(
                pb.merchant.MerchantCommand.UpdateMerchantStatusRequest.newBuilder()
                        .setMerchantId(id)
                        .setStatus(status == null ? "" : status)
                        .build())
                .map(UpdateMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update merchant status: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteMerchantPermanent(int id) {
        return telemetryHelper.traceAndMetric("merchant.deleteMerchantPermanent", () -> merchantCommandService.deleteMerchantPermanent(
                pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                        .setMerchantId(id)
                        .build())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedMerchantResponse> deleteMerchant(int id) {
        return telemetryHelper.traceAndMetric("merchant.deleteMerchant", () -> merchantCommandService.trashedMerchant(
                pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                        .setMerchantId(id)
                        .build())
                .map(TrashedMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedMerchantResponse> restoreMerchant(int id) {
        return telemetryHelper.traceAndMetric("merchant.restoreMerchant", () -> merchantCommandService.restoreMerchant(
                pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder()
                        .setMerchantId(id)
                        .build())
                .map(TrashedMerchantResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore merchant: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> restoreAllMerchants() {
        return telemetryHelper.traceAndMetric("merchant.restoreAllMerchants", () -> merchantCommandService.restoreAllMerchant(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteAllMerchants() {
        return telemetryHelper.traceAndMetric("merchant.deleteAllMerchants", () -> merchantCommandService.deleteAllMerchantPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all merchants: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponsePaginationMerchantTransaction> findAllTransactions(int page, int size, String search, int merchantId) {
        return telemetryHelper.traceAndMetric("merchant.findAllTransactions", () -> merchantTransactionService.findAllTransactionMerchant(
                pb.merchant.Merchant.FindAllMerchantTransaction.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .setMerchantId(merchantId)
                        .build())
                .map(ApiResponsePaginationMerchantTransaction::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponsePaginationMerchantTransaction> findTransactionsById(int page, int size, String search, String id) {
        return telemetryHelper.traceAndMetric("merchant.findTransactionsById", () -> merchantTransactionService.findAllTransactionByMerchant(
                pb.merchant.Merchant.FindAllMerchantTransactionId.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .setId(id == null ? "" : id)
                        .build())
                .map(ApiResponsePaginationMerchantTransaction::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get transactions by id: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponsePaginationMerchantTransaction> findTransactionsByApiKey(int page, int size, String search, String apiKey) {
        return telemetryHelper.traceAndMetric("merchant.findTransactionsByApiKey", () -> merchantTransactionService.findAllTransactionByApikey(
                pb.merchant.Merchant.FindAllMerchantTransactionApikey.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .setApiKey(apiKey == null ? "" : apiKey)
                        .build())
                .map(ApiResponsePaginationMerchantTransaction::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get transactions by api key: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyAmount> getMonthlyAmount(int year) {
        return telemetryHelper.traceAndMetric("merchant.getMonthlyAmount", () -> merchantStatsAmountService.findMonthlyAmountMerchant(
                pb.merchant.Merchant.FindYearMerchant.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseMerchantMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly amount: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantYearlyAmount> getYearlyAmount(int year) {
        return telemetryHelper.traceAndMetric("merchant.getYearlyAmount", () -> merchantStatsAmountService.findYearlyAmountMerchant(
                pb.merchant.Merchant.FindYearMerchant.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseMerchantYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly amount: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyAmount> getMonthlyAmountById(int year, int merchantId) {
        return telemetryHelper.traceAndMetric("merchant.getMonthlyAmountById", () -> merchantStatsAmountService.findMonthlyAmountByMerchants(
                pb.merchant.Merchant.FindYearMerchantById.newBuilder()
                        .setYear(year)
                        .setMerchantId(merchantId)
                        .build())
                .map(ApiResponseMerchantMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly amount by id: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantYearlyAmount> getYearlyAmountById(int year, int merchantId) {
        return telemetryHelper.traceAndMetric("merchant.getYearlyAmountById", () -> merchantStatsAmountService.findYearlyAmountByMerchants(
                pb.merchant.Merchant.FindYearMerchantById.newBuilder()
                        .setYear(year)
                        .setMerchantId(merchantId)
                        .build())
                .map(ApiResponseMerchantYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly amount by id: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyAmount> getMonthlyAmountByApiKey(int year, String apiKey) {
        return telemetryHelper.traceAndMetric("merchant.getMonthlyAmountByApiKey", () -> merchantStatsAmountService.findMonthlyAmountByApikey(
                pb.merchant.Merchant.FindYearMerchantByApikey.newBuilder()
                        .setYear(year)
                        .setApiKey(apiKey == null ? "" : apiKey)
                        .build())
                .map(ApiResponseMerchantMonthlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly amount by api key: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantYearlyAmount> getYearlyAmountByApiKey(int year, String apiKey) {
        return telemetryHelper.traceAndMetric("merchant.getYearlyAmountByApiKey", () -> merchantStatsAmountService.findYearlyAmountByApikey(
                pb.merchant.Merchant.FindYearMerchantByApikey.newBuilder()
                        .setYear(year)
                        .setApiKey(apiKey == null ? "" : apiKey)
                        .build())
                .map(ApiResponseMerchantYearlyAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly amount by api key: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyPaymentMethod> getMonthlyMethod(int year) {
        return telemetryHelper.traceAndMetric("merchant.getMonthlyMethod", () -> merchantStatsMethodService.findMonthlyPaymentMethodsMerchant(
                pb.merchant.Merchant.FindYearMerchant.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseMerchantMonthlyPaymentMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly method: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantYearlyPaymentMethod> getYearlyMethod(int year) {
        return telemetryHelper.traceAndMetric("merchant.getYearlyMethod", () -> merchantStatsMethodService.findYearlyPaymentMethodMerchant(
                pb.merchant.Merchant.FindYearMerchant.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseMerchantYearlyPaymentMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly method: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyPaymentMethod> getMonthlyMethodById(int year, int merchantId) {
        return telemetryHelper.traceAndMetric("merchant.getMonthlyMethodById", () -> merchantStatsMethodService.findMonthlyPaymentMethodByMerchants(
                pb.merchant.Merchant.FindYearMerchantById.newBuilder()
                        .setYear(year)
                        .setMerchantId(merchantId)
                        .build())
                .map(ApiResponseMerchantMonthlyPaymentMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly method by id: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantYearlyPaymentMethod> getYearlyMethodById(int year, int merchantId) {
        return telemetryHelper.traceAndMetric("merchant.getYearlyMethodById", () -> merchantStatsMethodService.findYearlyPaymentMethodByMerchants(
                pb.merchant.Merchant.FindYearMerchantById.newBuilder()
                        .setYear(year)
                        .setMerchantId(merchantId)
                        .build())
                .map(ApiResponseMerchantYearlyPaymentMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly method by id: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyPaymentMethod> getMonthlyMethodByApiKey(int year, String apiKey) {
        return telemetryHelper.traceAndMetric("merchant.getMonthlyMethodByApiKey", () -> merchantStatsMethodService.findMonthlyPaymentMethodByApikey(
                pb.merchant.Merchant.FindYearMerchantByApikey.newBuilder()
                        .setYear(year)
                        .setApiKey(apiKey == null ? "" : apiKey)
                        .build())
                .map(ApiResponseMerchantMonthlyPaymentMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly method by api key: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantYearlyPaymentMethod> getYearlyMethodByApiKey(int year, String apiKey) {
        return telemetryHelper.traceAndMetric("merchant.getYearlyMethodByApiKey", () -> merchantStatsMethodService.findYearlyPaymentMethodByApikey(
                pb.merchant.Merchant.FindYearMerchantByApikey.newBuilder()
                        .setYear(year)
                        .setApiKey(apiKey == null ? "" : apiKey)
                        .build())
                .map(ApiResponseMerchantYearlyPaymentMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly method by api key: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyTotalAmount> getTotalMonthlyAmount(int year) {
        return telemetryHelper.traceAndMetric("merchant.getTotalMonthlyAmount", () -> merchantStatsTotalAmountService.findMonthlyTotalAmountMerchant(
                pb.merchant.Merchant.FindYearMerchant.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseMerchantMonthlyTotalAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get total monthly amount: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantYearlyTotalAmount> getTotalYearlyAmount(int year) {
        return telemetryHelper.traceAndMetric("merchant.getTotalYearlyAmount", () -> merchantStatsTotalAmountService.findYearlyTotalAmountMerchant(
                pb.merchant.Merchant.FindYearMerchant.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseMerchantYearlyTotalAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get total yearly amount: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyTotalAmount> getTotalMonthlyAmountById(int year, int merchantId) {
        return telemetryHelper.traceAndMetric("merchant.getTotalMonthlyAmountById", () -> merchantStatsTotalAmountService.findMonthlyTotalAmountByMerchants(
                pb.merchant.Merchant.FindYearMerchantById.newBuilder()
                        .setYear(year)
                        .setMerchantId(merchantId)
                        .build())
                .map(ApiResponseMerchantMonthlyTotalAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get total monthly amount by id: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantYearlyTotalAmount> getTotalYearlyAmountById(int year, int merchantId) {
        return telemetryHelper.traceAndMetric("merchant.getTotalYearlyAmountById", () -> merchantStatsTotalAmountService.findYearlyTotalAmountByMerchants(
                pb.merchant.Merchant.FindYearMerchantById.newBuilder()
                        .setYear(year)
                        .setMerchantId(merchantId)
                        .build())
                .map(ApiResponseMerchantYearlyTotalAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get total yearly amount by id: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyTotalAmount> getTotalMonthlyAmountByApiKey(int year, String apiKey) {
        return telemetryHelper.traceAndMetric("merchant.getTotalMonthlyAmountByApiKey", () -> merchantStatsTotalAmountService.findMonthlyTotalAmountByApikey(
                pb.merchant.Merchant.FindYearMerchantByApikey.newBuilder()
                        .setYear(year)
                        .setApiKey(apiKey == null ? "" : apiKey)
                        .build())
                .map(ApiResponseMerchantMonthlyTotalAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get total monthly amount by api key: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMerchantYearlyTotalAmount> getTotalYearlyAmountByApiKey(int year, String apiKey) {
        return telemetryHelper.traceAndMetric("merchant.getTotalYearlyAmountByApiKey", () -> merchantStatsTotalAmountService.findYearlyTotalAmountByApikey(
                pb.merchant.Merchant.FindYearMerchantByApikey.newBuilder()
                        .setYear(year)
                        .setApiKey(apiKey == null ? "" : apiKey)
                        .build())
                .map(ApiResponseMerchantYearlyTotalAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get total yearly amount by api key: " + throwable.getMessage(), throwable)));
    }
}
