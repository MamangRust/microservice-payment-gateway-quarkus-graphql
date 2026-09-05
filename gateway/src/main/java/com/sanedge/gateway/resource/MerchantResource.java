package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.MerchantDto.*;
import com.sanedge.gateway.service.MerchantService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class MerchantResource {

    @Inject
    MerchantService merchantService;

    @Query("listMerchants")
    @Description("List all merchants")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllMerchantResponse> listMerchants(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return merchantService.listMerchants(page, size, search);
    }

    @Query("getMerchant")
    @Description("Get merchant by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdMerchantResponse> getMerchant(@Name("id") int id) {
        return merchantService.getMerchant(id);
    }

    @Query("getMerchantByApiKey")
    @Description("Get merchant by API key")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdMerchantResponse> getMerchantByApiKey(@Name("apiKey") String apiKey) {
        return merchantService.getMerchantByApiKey(apiKey);
    }

    @Query("getMerchantsByUserId")
    @Description("Get merchants by User ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindAllMerchantResponse> getMerchantsByUserId(@Name("userId") int userId) {
        return merchantService.getMerchantsByUserId(userId);
    }

    @Query("findActiveMerchants")
    @Description("List active merchants")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllMerchantResponse> findActiveMerchants(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return merchantService.findActiveMerchants(page, size, search);
    }

    @Query("findTrashedMerchants")
    @Description("List trashed merchants")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllMerchantResponse> findTrashedMerchants(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return merchantService.findTrashedMerchants(page, size, search);
    }

    @Mutation("createMerchant")
    @Description("Create a new merchant")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<CreateMerchantResponse> createMerchant(@Name("body") CreateMerchantRequest body) {
        return merchantService.createMerchant(body);
    }

    @Mutation("updateMerchant")
    @Description("Update merchant")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<UpdateMerchantResponse> updateMerchant(@Name("id") int id, @Name("body") UpdateMerchantRequest body) {
        return merchantService.updateMerchant(id, body);
    }

    @Mutation("updateMerchantStatus")
    @Description("Update merchant status")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<UpdateMerchantResponse> updateMerchantStatus(@Name("id") int id, @Name("status") String status) {
        return merchantService.updateMerchantStatus(id, status);
    }

    @Mutation("deleteMerchantPermanent")
    @Description("Permanently delete a merchant")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteMerchantPermanent(@Name("id") int id) {
        return merchantService.deleteMerchantPermanent(id);
    }

    @Mutation("deleteMerchant")
    @Description("Soft-delete a merchant")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedMerchantResponse> deleteMerchant(@Name("id") int id) {
        return merchantService.deleteMerchant(id);
    }

    @Mutation("restoreMerchant")
    @Description("Restore a soft-deleted merchant")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedMerchantResponse> restoreMerchant(@Name("id") int id) {
        return merchantService.restoreMerchant(id);
    }

    @Mutation("restoreAllMerchants")
    @Description("Restore all soft-deleted merchants")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> restoreAllMerchants() {
        return merchantService.restoreAllMerchants();
    }

    @Mutation("deleteAllMerchants")
    @Description("Delete all merchants permanently")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteAllMerchants() {
        return merchantService.deleteAllMerchants();
    }

    @Query("findAllMerchantTransactions")
    @Description("List all merchant transactions")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponsePaginationMerchantTransaction> findAllTransactions(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search,
            @Name("merchantId") int merchantId) {
        return merchantService.findAllTransactions(page, size, search, merchantId);
    }

    @Query("findTransactionsById")
    @Description("Find merchant transactions by id")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponsePaginationMerchantTransaction> findTransactionsById(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search,
            @Name("id") String id) {
        return merchantService.findTransactionsById(page, size, search, id);
    }

    @Query("findTransactionsByApiKey")
    @Description("Find merchant transactions by API key")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponsePaginationMerchantTransaction> findTransactionsByApiKey(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search,
            @Name("apiKey") String apiKey) {
        return merchantService.findTransactionsByApiKey(page, size, search, apiKey);
    }

    @Query("getMonthlyAmount")
    @Description("Get monthly amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMerchantMonthlyAmount> getMonthlyAmount(@Name("year") int year) {
        return merchantService.getMonthlyAmount(year);
    }

    @Query("getYearlyAmount")
    @Description("Get yearly amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMerchantYearlyAmount> getYearlyAmount(@Name("year") int year) {
        return merchantService.getYearlyAmount(year);
    }

    @Query("getMonthlyAmountById")
    @Description("Get monthly amount statistics by merchant ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantMonthlyAmount> getMonthlyAmountById(
            @Name("year") int year,
            @Name("merchantId") int merchantId) {
        return merchantService.getMonthlyAmountById(year, merchantId);
    }

    @Query("getYearlyAmountById")
    @Description("Get yearly amount statistics by merchant ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantYearlyAmount> getYearlyAmountById(
            @Name("year") int year,
            @Name("merchantId") int merchantId) {
        return merchantService.getYearlyAmountById(year, merchantId);
    }

    @Query("getMonthlyAmountByApiKey")
    @Description("Get monthly amount statistics by API key")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantMonthlyAmount> getMonthlyAmountByApiKey(
            @Name("year") int year,
            @Name("apiKey") String apiKey) {
        return merchantService.getMonthlyAmountByApiKey(year, apiKey);
    }

    @Query("getYearlyAmountByApiKey")
    @Description("Get yearly amount statistics by API key")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantYearlyAmount> getYearlyAmountByApiKey(
            @Name("year") int year,
            @Name("apiKey") String apiKey) {
        return merchantService.getYearlyAmountByApiKey(year, apiKey);
    }

    @Query("getMonthlyMethod")
    @Description("Get monthly merchant transaction method statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMerchantMonthlyPaymentMethod> getMonthlyMethod(
            @Name("year") int year) {
        return merchantService.getMonthlyMethod(year);
    }

    @Query("getYearlyMethod")
    @Description("Get yearly merchant transaction method statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMerchantYearlyPaymentMethod> getYearlyMethod(@Name("year") int year) {
        return merchantService.getYearlyMethod(year);
    }

    @Query("getMonthlyMethodById")
    @Description("Get monthly merchant transaction method statistics by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantMonthlyPaymentMethod> getMonthlyMethodById(
            @Name("year") int year,
            @Name("merchantId") int merchantId) {
        return merchantService.getMonthlyMethodById(year, merchantId);
    }

    @Query("getYearlyMethodById")
    @Description("Get yearly merchant transaction method statistics by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantYearlyPaymentMethod> getYearlyMethodById(
            @Name("year") int year,
            @Name("merchantId") int merchantId) {
        return merchantService.getYearlyMethodById(year, merchantId);
    }

    @Query("getMonthlyMethodByApiKey")
    @Description("Get monthly merchant transaction method statistics by API key")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantMonthlyPaymentMethod> getMonthlyMethodByApiKey(
            @Name("year") int year,
            @Name("apiKey") String apiKey) {
        return merchantService.getMonthlyMethodByApiKey(year, apiKey);
    }

    @Query("getYearlyMethodByApiKey")
    @Description("Get yearly merchant transaction method statistics by API key")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantYearlyPaymentMethod> getYearlyMethodByApiKey(
            @Name("year") int year,
            @Name("apiKey") String apiKey) {
        return merchantService.getYearlyMethodByApiKey(year, apiKey);
    }

    @Query("getTotalMonthlyAmount")
    @Description("Get monthly total transaction amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMerchantMonthlyTotalAmount> getTotalMonthlyAmount(@Name("year") int year) {
        return merchantService.getTotalMonthlyAmount(year);
    }

    @Query("getTotalYearlyAmount")
    @Description("Get yearly total transaction amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMerchantYearlyTotalAmount> getTotalYearlyAmount(@Name("year") int year) {
        return merchantService.getTotalYearlyAmount(year);
    }

    @Query("getTotalMonthlyAmountById")
    @Description("Get monthly total transaction amount statistics by merchant ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantMonthlyTotalAmount> getTotalMonthlyAmountById(
            @Name("year") int year,
            @Name("merchantId") int merchantId) {
        return merchantService.getTotalMonthlyAmountById(year, merchantId);
    }

    @Query("getTotalYearlyAmountById")
    @Description("Get yearly total transaction amount statistics by merchant ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantYearlyTotalAmount> getTotalYearlyAmountById(
            @Name("year") int year,
            @Name("merchantId") int merchantId) {
        return merchantService.getTotalYearlyAmountById(year, merchantId);
    }

    @Query("getTotalMonthlyAmountByApiKey")
    @Description("Get monthly total transaction amount statistics by API key")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantMonthlyTotalAmount> getTotalMonthlyAmountByApiKey(
            @Name("year") int year,
            @Name("apiKey") String apiKey) {
        return merchantService.getTotalMonthlyAmountByApiKey(year, apiKey);
    }

    @Query("getTotalYearlyAmountByApiKey")
    @Description("Get yearly total transaction amount statistics by API key")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMerchantYearlyTotalAmount> getTotalYearlyAmountByApiKey(
            @Name("year") int year,
            @Name("apiKey") String apiKey) {
        return merchantService.getTotalYearlyAmountByApiKey(year, apiKey);
    }
}
