package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.TopupDto.*;
import io.smallrye.mutiny.Uni;

public interface TopupService {
    Uni<FindAllTopupResponse> listTopups(int page, int size, String search);
    Uni<FindAllTopupResponse> listTopupsByCard(String cardNumber, int page, int size, String search);
    Uni<FindAllTopupResponse> findActiveTopups(int page, int size, String search);
    Uni<FindAllTopupResponse> findTrashedTopups(int page, int size, String search);
    Uni<FindByIdTopupResponse> getTopup(int id);
    Uni<FindByIdTopupResponse> getTopupByCard(String cardNumber, int year);
    Uni<CreateTopupResponse> createTopup(CreateTopupRequest body);
    Uni<UpdateTopupResponse> updateTopup(int id, UpdateTopupRequest body);
    Uni<SimpleStatusMessageResponse> deleteTopupPermanent(int id);
    Uni<TrashedTopupResponse> trashTopup(int id);
    Uni<TrashedTopupResponse> restoreTopup(int id);
    Uni<SimpleStatusMessageResponse> restoreAllTopups();
    Uni<SimpleStatusMessageResponse> deleteAllTopups();

    // ── Stats methods (typed DTOs, statsreader gRPC) ──

    Uni<ApiResponseTopupMonthAmount> findMonthlyAmounts(int year);
    Uni<ApiResponseTopupYearAmount> findYearlyAmounts(int year);
    Uni<ApiResponseTopupMonthAmount> findMonthlyAmountsByCard(int year, String cardNumber);
    Uni<ApiResponseTopupYearAmount> findYearlyAmountsByCard(int year, String cardNumber);

    Uni<ApiResponseTopupMonthMethod> findMonthlyMethods(int year, int month);
    Uni<ApiResponseTopupYearMethod> findYearlyMethods(int year);
    Uni<ApiResponseTopupMonthMethod> findMonthlyMethodsByCard(int year, int month, String cardNumber);
    Uni<ApiResponseTopupYearMethod> findYearlyMethodsByCard(int year, String cardNumber);

    Uni<ApiResponseTopupMonthStatusSuccess> findMonthlyStatusSuccess(int year, int month);
    Uni<ApiResponseTopupYearStatusSuccess> findYearlyStatusSuccess(int year);
    Uni<ApiResponseTopupMonthStatusFailed> findMonthlyStatusFailed(int year, int month);
    Uni<ApiResponseTopupYearStatusFailed> findYearlyStatusFailed(int year);

    Uni<ApiResponseTopupMonthStatusSuccess> findMonthlyStatusSuccessByCard(int year, int month, String cardNumber);
    Uni<ApiResponseTopupYearStatusSuccess> findYearlyStatusSuccessByCard(int year, String cardNumber);
    Uni<ApiResponseTopupMonthStatusFailed> findMonthlyStatusFailedByCard(int year, int month, String cardNumber);
    Uni<ApiResponseTopupYearStatusFailed> findYearlyStatusFailedByCard(int year, String cardNumber);
}
