package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantDto.*;
import io.smallrye.mutiny.Uni;

public interface MerchantService {
    Uni<FindAllMerchantResponse> listMerchants(int page, int size, String search);
    Uni<FindByIdMerchantResponse> getMerchant(int id);
    Uni<FindByIdMerchantResponse> getMerchantByApiKey(String apiKey);
    Uni<FindAllMerchantResponse> getMerchantsByUserId(int userId);
    Uni<FindAllMerchantResponse> findActiveMerchants(int page, int size, String search);
    Uni<FindAllMerchantResponse> findTrashedMerchants(int page, int size, String search);
    Uni<CreateMerchantResponse> createMerchant(CreateMerchantRequest body);
    Uni<UpdateMerchantResponse> updateMerchant(int id, UpdateMerchantRequest body);
    Uni<UpdateMerchantResponse> updateMerchantStatus(int id, String status);
    Uni<SimpleStatusMessageResponse> deleteMerchantPermanent(int id);
    Uni<TrashedMerchantResponse> deleteMerchant(int id);
    Uni<TrashedMerchantResponse> restoreMerchant(int id);
    Uni<SimpleStatusMessageResponse> restoreAllMerchants();
    Uni<SimpleStatusMessageResponse> deleteAllMerchants();

    Uni<ApiResponsePaginationMerchantTransaction> findAllTransactions(int page, int size, String search, int merchantId);
    Uni<ApiResponsePaginationMerchantTransaction> findTransactionsById(int page, int size, String search, String id);
    Uni<ApiResponsePaginationMerchantTransaction> findTransactionsByApiKey(int page, int size, String search, String apiKey);

    Uni<ApiResponseMerchantMonthlyAmount> getMonthlyAmount(int year);
    Uni<ApiResponseMerchantYearlyAmount> getYearlyAmount(int year);
    Uni<ApiResponseMerchantMonthlyAmount> getMonthlyAmountById(int year, int merchantId);
    Uni<ApiResponseMerchantYearlyAmount> getYearlyAmountById(int year, int merchantId);
    Uni<ApiResponseMerchantMonthlyAmount> getMonthlyAmountByApiKey(int year, String apiKey);
    Uni<ApiResponseMerchantYearlyAmount> getYearlyAmountByApiKey(int year, String apiKey);

    Uni<ApiResponseMerchantMonthlyPaymentMethod> getMonthlyMethod(int year);
    Uni<ApiResponseMerchantYearlyPaymentMethod> getYearlyMethod(int year);
    Uni<ApiResponseMerchantMonthlyPaymentMethod> getMonthlyMethodById(int year, int merchantId);
    Uni<ApiResponseMerchantYearlyPaymentMethod> getYearlyMethodById(int year, int merchantId);
    Uni<ApiResponseMerchantMonthlyPaymentMethod> getMonthlyMethodByApiKey(int year, String apiKey);
    Uni<ApiResponseMerchantYearlyPaymentMethod> getYearlyMethodByApiKey(int year, String apiKey);

    Uni<ApiResponseMerchantMonthlyTotalAmount> getTotalMonthlyAmount(int year);
    Uni<ApiResponseMerchantYearlyTotalAmount> getTotalYearlyAmount(int year);
    Uni<ApiResponseMerchantMonthlyTotalAmount> getTotalMonthlyAmountById(int year, int merchantId);
    Uni<ApiResponseMerchantYearlyTotalAmount> getTotalYearlyAmountById(int year, int merchantId);
    Uni<ApiResponseMerchantMonthlyTotalAmount> getTotalMonthlyAmountByApiKey(int year, String apiKey);
    Uni<ApiResponseMerchantYearlyTotalAmount> getTotalYearlyAmountByApiKey(int year, String apiKey);
}
