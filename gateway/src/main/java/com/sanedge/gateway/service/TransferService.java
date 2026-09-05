package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.TransferDto.*;
import io.smallrye.mutiny.Uni;

public interface TransferService {
    Uni<FindAllTransferResponse> listTransfers(int page, int size, String search);
    Uni<FindAllTransferResponse> findActiveTransfers(int page, int size, String search);
    Uni<FindAllTransferResponse> findTrashedTransfers(int page, int size, String search);
    Uni<FindByIdTransferResponse> getTransfer(int id);
    Uni<FindAllTransferResponse> findTransfersFrom(String cardNumber);
    Uni<FindAllTransferResponse> findTransfersTo(String cardNumber);
    Uni<CreateTransferResponse> createTransfer(CreateTransferRequest body);
    Uni<UpdateTransferResponse> updateTransfer(int id, UpdateTransferRequest body);
    Uni<SimpleStatusMessageResponse> deleteTransferPermanent(int id);
    Uni<TrashedTransferResponse> trashTransfer(int id);
    Uni<TrashedTransferResponse> restoreTransfer(int id);
    Uni<SimpleStatusMessageResponse> restoreAllTransfers();
    Uni<SimpleStatusMessageResponse> deleteAllTransfers();

    Uni<ApiResponseTransferMonthAmount> findMonthlyAmounts(int year);
    Uni<ApiResponseTransferYearAmount> findYearlyAmounts(int year);
    Uni<ApiResponseTransferMonthAmount> findMonthlyAmountsFromCard(int year, String cardNumber);
    Uni<ApiResponseTransferMonthAmount> findMonthlyAmountsToCard(int year, String cardNumber);
    Uni<ApiResponseTransferYearAmount> findYearlyAmountsFromCard(int year, String cardNumber);
    Uni<ApiResponseTransferYearAmount> findYearlyAmountsToCard(int year, String cardNumber);

    Uni<ApiResponseTransferMonthStatusSuccess> findMonthlyStatusSuccess(int year, int month);
    Uni<ApiResponseTransferYearStatusSuccess> findYearlyStatusSuccess(int year);
    Uni<ApiResponseTransferMonthStatusFailed> findMonthlyStatusFailed(int year, int month);
    Uni<ApiResponseTransferYearStatusFailed> findYearlyStatusFailed(int year);

    Uni<ApiResponseTransferMonthStatusSuccess> findMonthlyStatusSuccessByCard(int year, int month, String cardNumber);
    Uni<ApiResponseTransferYearStatusSuccess> findYearlyStatusSuccessByCard(int year, String cardNumber);
    Uni<ApiResponseTransferMonthStatusFailed> findMonthlyStatusFailedByCard(int year, int month, String cardNumber);
    Uni<ApiResponseTransferYearStatusFailed> findYearlyStatusFailedByCard(int year, String cardNumber);
}
