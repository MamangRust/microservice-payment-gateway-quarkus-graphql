package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.TransferDto.*;
import com.sanedge.gateway.service.TransferService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class TransferResource {

    @Inject
    TransferService transferService;

    @Query("listTransfers")
    @Description("List all transfers")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllTransferResponse> listTransfers(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return transferService.listTransfers(page, size, search);
    }

    @Query("findActiveTransfers")
    @Description("List active transfers")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllTransferResponse> findActiveTransfers(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return transferService.findActiveTransfers(page, size, search);
    }

    @Query("findTrashedTransfers")
    @Description("List trashed transfers")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllTransferResponse> findTrashedTransfers(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return transferService.findTrashedTransfers(page, size, search);
    }

    @Query("getTransfer")
    @Description("Get transfer by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdTransferResponse> getTransfer(@Name("id") int id) {
        return transferService.getTransfer(id);
    }

    @Query("findTransfersFrom")
    @Description("Get transfers from card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindAllTransferResponse> findTransfersFrom(@Name("cardNumber") String cardNumber) {
        return transferService.findTransfersFrom(cardNumber);
    }

    @Query("findTransfersTo")
    @Description("Get transfers to card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindAllTransferResponse> findTransfersTo(@Name("cardNumber") String cardNumber) {
        return transferService.findTransfersTo(cardNumber);
    }

    @Mutation("createTransfer")
    @Description("Create a new transfer")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<CreateTransferResponse> createTransfer(@Name("body") CreateTransferRequest body) {
        return transferService.createTransfer(body);
    }

    @Mutation("updateTransfer")
    @Description("Update transfer")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<UpdateTransferResponse> updateTransfer(@Name("id") int id, @Name("body") UpdateTransferRequest body) {
        return transferService.updateTransfer(id, body);
    }

    @Mutation("deleteTransferPermanent")
    @Description("Permanently delete a transfer")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteTransferPermanent(@Name("id") int id) {
        return transferService.deleteTransferPermanent(id);
    }

    @Mutation("trashTransfer")
    @Description("Soft-delete transfer by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedTransferResponse> trashTransfer(@Name("id") int id) {
        return transferService.trashTransfer(id);
    }

    @Mutation("restoreTransfer")
    @Description("Restore transfer by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedTransferResponse> restoreTransfer(@Name("id") int id) {
        return transferService.restoreTransfer(id);
    }

    @Mutation("restoreAllTransfers")
    @Description("Restore all transfers")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> restoreAllTransfers() {
        return transferService.restoreAllTransfers();
    }

    @Mutation("deleteAllTransfers")
    @Description("Delete all transfers permanently")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteAllTransfers() {
        return transferService.deleteAllTransfers();
    }

    @Query("findMonthlyTransferAmounts")
    @Description("Get monthly transfer amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransferMonthAmount> findMonthlyAmounts(@Name("year") int year) {
        return transferService.findMonthlyAmounts(year);
    }

    @Query("findYearlyTransferAmounts")
    @Description("Get yearly transfer amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransferYearAmount> findYearlyAmounts(@Name("year") int year) {
        return transferService.findYearlyAmounts(year);
    }

    @Query("findMonthlyTransferAmountsFromCard")
    @Description("Get monthly transfer amount statistics from card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransferMonthAmount> findMonthlyAmountsFromCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transferService.findMonthlyAmountsFromCard(year, cardNumber);
    }

    @Query("findMonthlyTransferAmountsToCard")
    @Description("Get monthly transfer amount statistics to card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransferMonthAmount> findMonthlyAmountsToCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transferService.findMonthlyAmountsToCard(year, cardNumber);
    }

    @Query("findYearlyTransferAmountsFromCard")
    @Description("Get yearly transfer amount statistics from card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransferYearAmount> findYearlyAmountsFromCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transferService.findYearlyAmountsFromCard(year, cardNumber);
    }

    @Query("findYearlyTransferAmountsToCard")
    @Description("Get yearly transfer amount statistics to card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransferYearAmount> findYearlyAmountsToCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transferService.findYearlyAmountsToCard(year, cardNumber);
    }

    @Query("findMonthlyTransferStatusSuccess")
    @Description("Get monthly transfer status success statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransferMonthStatusSuccess> findMonthlyStatusSuccess(
            @Name("year") int year,
            @Name("month") int month) {
        return transferService.findMonthlyStatusSuccess(year, month);
    }

    @Query("findYearlyTransferStatusSuccess")
    @Description("Get yearly transfer status success statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransferYearStatusSuccess> findYearlyStatusSuccess(@Name("year") int year) {
        return transferService.findYearlyStatusSuccess(year);
    }

    @Query("findMonthlyTransferStatusFailed")
    @Description("Get monthly transfer status failed statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransferMonthStatusFailed> findMonthlyStatusFailed(
            @Name("year") int year,
            @Name("month") int month) {
        return transferService.findMonthlyStatusFailed(year, month);
    }

    @Query("findYearlyTransferStatusFailed")
    @Description("Get yearly transfer status failed statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransferYearStatusFailed> findYearlyStatusFailed(@Name("year") int year) {
        return transferService.findYearlyStatusFailed(year);
    }

    @Query("findMonthlyTransferStatusSuccessByCard")
    @Description("Get monthly transfer status success statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransferMonthStatusSuccess> findMonthlyStatusSuccessByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return transferService.findMonthlyStatusSuccessByCard(year, month, cardNumber);
    }

    @Query("findYearlyTransferStatusSuccessByCard")
    @Description("Get yearly transfer status success statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransferYearStatusSuccess> findYearlyStatusSuccessByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transferService.findYearlyStatusSuccessByCard(year, cardNumber);
    }

    @Query("findMonthlyTransferStatusFailedByCard")
    @Description("Get monthly transfer status failed statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransferMonthStatusFailed> findMonthlyStatusFailedByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return transferService.findMonthlyStatusFailedByCard(year, month, cardNumber);
    }

    @Query("findYearlyTransferStatusFailedByCard")
    @Description("Get yearly transfer status failed statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransferYearStatusFailed> findYearlyStatusFailedByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transferService.findYearlyStatusFailedByCard(year, cardNumber);
    }
}
