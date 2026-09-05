package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.TransactionDto.*;
import com.sanedge.gateway.service.TransactionService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    @Query("listTransactions")
    @Description("List all transactions")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllTransactionResponse> listTransactions(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return transactionService.listTransactions(page, size, search);
    }

    @Query("findTransactionsByCard")
    @Description("List transactions by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindAllTransactionResponse> findTransactionsByCard(@Name("cardNumber") String cardNumber) {
        return transactionService.findTransactionsByCard(cardNumber);
    }

    @Query("findTransactionsByMerchant")
    @Description("List transactions by merchant ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindAllTransactionResponse> findTransactionsByMerchant(@Name("merchantId") int merchantId) {
        return transactionService.findTransactionsByMerchant(merchantId);
    }

    @Query("findTransactionsByCardAndYear")
    @Description("List transactions by card number and year")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindAllTransactionResponse> findTransactionsByCardAndYear(
            @Name("cardNumber") String cardNumber,
            @Name("year") int year) {
        return transactionService.findTransactionsByCardAndYear(cardNumber, year);
    }

    @Query("findActiveTransactions")
    @Description("List active transactions")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllTransactionResponse> findActiveTransactions(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return transactionService.findActiveTransactions(page, size, search);
    }

    @Query("findTrashedTransactions")
    @Description("List trashed transactions")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllTransactionResponse> findTrashedTransactions(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return transactionService.findTrashedTransactions(page, size, search);
    }

    @Query("getTransaction")
    @Description("Get transaction by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdTransactionResponse> getTransaction(@Name("id") int id) {
        return transactionService.getTransaction(id);
    }

    @Mutation("createTransaction")
    @Description("Create a new transaction")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<CreateTransactionResponse> createTransaction(@Name("body") CreateTransactionBody body) {
        return transactionService.createTransaction(body);
    }

    @Mutation("updateTransaction")
    @Description("Update transaction")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<UpdateTransactionResponse> updateTransaction(@Name("id") int id, @Name("body") UpdateTransactionBody body) {
        return transactionService.updateTransaction(id, body);
    }

    @Mutation("deleteTransactionPermanent")
    @Description("Permanently delete transaction by ID")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteTransactionPermanent(@Name("id") int id) {
        return transactionService.deleteTransactionPermanent(id);
    }

    @Mutation("trashTransaction")
    @Description("Soft-delete transaction by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedTransactionResponse> trashTransaction(@Name("id") int id) {
        return transactionService.trashTransaction(id);
    }

    @Mutation("restoreTransaction")
    @Description("Restore transaction by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedTransactionResponse> restoreTransaction(@Name("id") int id) {
        return transactionService.restoreTransaction(id);
    }

    @Mutation("restoreAllTransactions")
    @Description("Restore all transactions")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> restoreAllTransactions() {
        return transactionService.restoreAllTransactions();
    }

    @Mutation("deleteAllTransactions")
    @Description("Delete all transactions permanently")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteAllTransactions() {
        return transactionService.deleteAllTransactions();
    }

    @Query("findMonthlyTransactionAmounts")
    @Description("Get monthly transaction amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransactionMonthAmount> findMonthlyAmounts(@Name("year") int year) {
        return transactionService.findMonthlyAmounts(year);
    }

    @Query("findYearlyTransactionAmounts")
    @Description("Get yearly transaction amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransactionYearAmount> findYearlyAmounts(@Name("year") int year) {
        return transactionService.findYearlyAmounts(year);
    }

    @Query("findMonthlyTransactionAmountsByCard")
    @Description("Get monthly transaction amount statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransactionMonthAmount> findMonthlyAmountsByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transactionService.findMonthlyAmountsByCard(year, cardNumber);
    }

    @Query("findYearlyTransactionAmountsByCard")
    @Description("Get yearly transaction amount statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransactionYearAmount> findYearlyAmountsByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transactionService.findYearlyAmountsByCard(year, cardNumber);
    }

    @Query("findMonthlyTransactionMethods")
    @Description("Get monthly transaction method statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransactionMonthMethod> findMonthlyMethods(
            @Name("year") int year,
            @Name("month") int month) {
        return transactionService.findMonthlyMethods(year, month);
    }

    @Query("findYearlyTransactionMethods")
    @Description("Get yearly transaction method statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransactionYearMethod> findYearlyMethods(@Name("year") int year) {
        return transactionService.findYearlyMethods(year);
    }

    @Query("findMonthlyTransactionMethodsByCard")
    @Description("Get monthly transaction method statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransactionMonthMethod> findMonthlyMethodsByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return transactionService.findMonthlyMethodsByCard(year, month, cardNumber);
    }

    @Query("findYearlyTransactionMethodsByCard")
    @Description("Get yearly transaction method statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransactionYearMethod> findYearlyMethodsByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transactionService.findYearlyMethodsByCard(year, cardNumber);
    }

    @Query("findMonthlyTransactionStatusSuccess")
    @Description("Get monthly transaction status success statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransactionMonthStatusSuccess> findMonthlyStatusSuccess(
            @Name("year") int year,
            @Name("month") int month) {
        return transactionService.findMonthlyStatusSuccess(year, month);
    }

    @Query("findYearlyTransactionStatusSuccess")
    @Description("Get yearly transaction status success statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransactionYearStatusSuccess> findYearlyStatusSuccess(@Name("year") int year) {
        return transactionService.findYearlyStatusSuccess(year);
    }

    @Query("findMonthlyTransactionStatusFailed")
    @Description("Get monthly transaction status failed statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransactionMonthStatusFailed> findMonthlyStatusFailed(
            @Name("year") int year,
            @Name("month") int month) {
        return transactionService.findMonthlyStatusFailed(year, month);
    }

    @Query("findYearlyTransactionStatusFailed")
    @Description("Get yearly transaction status failed statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTransactionYearStatusFailed> findYearlyStatusFailed(@Name("year") int year) {
        return transactionService.findYearlyStatusFailed(year);
    }

    @Query("findMonthlyTransactionStatusSuccessByCard")
    @Description("Get monthly transaction status success statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransactionMonthStatusSuccess> findMonthlyStatusSuccessByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return transactionService.findMonthlyStatusSuccessByCard(year, month, cardNumber);
    }

    @Query("findYearlyTransactionStatusSuccessByCard")
    @Description("Get yearly transaction status success statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransactionYearStatusSuccess> findYearlyStatusSuccessByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transactionService.findYearlyStatusSuccessByCard(year, cardNumber);
    }

    @Query("findMonthlyTransactionStatusFailedByCard")
    @Description("Get monthly transaction status failed statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransactionMonthStatusFailed> findMonthlyStatusFailedByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return transactionService.findMonthlyStatusFailedByCard(year, month, cardNumber);
    }

    @Query("findYearlyTransactionStatusFailedByCard")
    @Description("Get yearly transaction status failed statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTransactionYearStatusFailed> findYearlyStatusFailedByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return transactionService.findYearlyStatusFailedByCard(year, cardNumber);
    }
}
