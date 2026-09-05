package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.TopupDto.*;
import com.sanedge.gateway.service.TopupService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class TopupResource {

    @Inject
    TopupService topupService;

    @Query("listTopups")
    @Description("List all topups")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllTopupResponse> listTopups(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return topupService.listTopups(page, size, search);
    }

    @Query("listTopupsByCard")
    @Description("List topups by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindAllTopupResponse> listTopupsByCard(
            @Name("cardNumber") String cardNumber,
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return topupService.listTopupsByCard(cardNumber, page, size, search);
    }

    @Query("findActiveTopups")
    @Description("List active topups")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllTopupResponse> findActiveTopups(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return topupService.findActiveTopups(page, size, search);
    }

    @Query("findTrashedTopups")
    @Description("List trashed topups")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllTopupResponse> findTrashedTopups(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return topupService.findTrashedTopups(page, size, search);
    }

    @Query("getTopup")
    @Description("Get topup by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdTopupResponse> getTopup(@Name("id") int id) {
        return topupService.getTopup(id);
    }

    @Query("getTopupByCard")
    @Description("Get topup by card number and year")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdTopupResponse> getTopupByCard(@Name("cardNumber") String cardNumber, @Name("year") int year) {
        return topupService.getTopupByCard(cardNumber, year);
    }

    @Mutation("createTopup")
    @Description("Create a new topup")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<CreateTopupResponse> createTopup(@Name("body") CreateTopupRequest body) {
        return topupService.createTopup(body);
    }

    @Mutation("updateTopup")
    @Description("Update topup")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<UpdateTopupResponse> updateTopup(@Name("id") int id, @Name("body") UpdateTopupRequest body) {
        return topupService.updateTopup(id, body);
    }

    @Mutation("deleteTopupPermanent")
    @Description("Permanently delete topup by ID")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteTopupPermanent(@Name("id") int id) {
        return topupService.deleteTopupPermanent(id);
    }

    @Mutation("trashTopup")
    @Description("Soft-delete topup by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedTopupResponse> trashTopup(@Name("id") int id) {
        return topupService.trashTopup(id);
    }

    @Mutation("restoreTopup")
    @Description("Restore topup by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedTopupResponse> restoreTopup(@Name("id") int id) {
        return topupService.restoreTopup(id);
    }

    @Mutation("restoreAllTopups")
    @Description("Restore all topups")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> restoreAllTopups() {
        return topupService.restoreAllTopups();
    }

    @Mutation("deleteAllTopups")
    @Description("Delete all topups permanently")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteAllTopups() {
        return topupService.deleteAllTopups();
    }

    @Query("findMonthlyTopupAmounts")
    @Description("Get monthly topup amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTopupMonthAmount> findMonthlyAmounts(@Name("year") int year) {
        return topupService.findMonthlyAmounts(year);
    }

    @Query("findYearlyTopupAmounts")
    @Description("Get yearly topup amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTopupYearAmount> findYearlyAmounts(@Name("year") int year) {
        return topupService.findYearlyAmounts(year);
    }

    @Query("findMonthlyTopupAmountsByCard")
    @Description("Get monthly topup amount statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTopupMonthAmount> findMonthlyAmountsByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return topupService.findMonthlyAmountsByCard(year, cardNumber);
    }

    @Query("findYearlyTopupAmountsByCard")
    @Description("Get yearly topup amount statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTopupYearAmount> findYearlyAmountsByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return topupService.findYearlyAmountsByCard(year, cardNumber);
    }

    @Query("findMonthlyTopupMethods")
    @Description("Get monthly topup method statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTopupMonthMethod> findMonthlyMethods(
            @Name("year") int year,
            @Name("month") int month) {
        return topupService.findMonthlyMethods(year, month);
    }

    @Query("findYearlyTopupMethods")
    @Description("Get yearly topup method statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTopupYearMethod> findYearlyMethods(@Name("year") int year) {
        return topupService.findYearlyMethods(year);
    }

    @Query("findMonthlyTopupMethodsByCard")
    @Description("Get monthly topup method statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTopupMonthMethod> findMonthlyMethodsByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return topupService.findMonthlyMethodsByCard(year, month, cardNumber);
    }

    @Query("findYearlyTopupMethodsByCard")
    @Description("Get yearly topup method statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTopupYearMethod> findYearlyMethodsByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return topupService.findYearlyMethodsByCard(year, cardNumber);
    }

    @Query("findMonthlyTopupStatusSuccess")
    @Description("Get monthly topup status success statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTopupMonthStatusSuccess> findMonthlyStatusSuccess(
            @Name("year") int year,
            @Name("month") int month) {
        return topupService.findMonthlyStatusSuccess(year, month);
    }

    @Query("findYearlyTopupStatusSuccess")
    @Description("Get yearly topup status success statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTopupYearStatusSuccess> findYearlyStatusSuccess(@Name("year") int year) {
        return topupService.findYearlyStatusSuccess(year);
    }

    @Query("findMonthlyTopupStatusFailed")
    @Description("Get monthly topup status failed statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTopupMonthStatusFailed> findMonthlyStatusFailed(
            @Name("year") int year,
            @Name("month") int month) {
        return topupService.findMonthlyStatusFailed(year, month);
    }

    @Query("findYearlyTopupStatusFailed")
    @Description("Get yearly topup status failed statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseTopupYearStatusFailed> findYearlyStatusFailed(@Name("year") int year) {
        return topupService.findYearlyStatusFailed(year);
    }

    @Query("findMonthlyTopupStatusSuccessByCard")
    @Description("Get monthly topup status success statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTopupMonthStatusSuccess> findMonthlyStatusSuccessByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return topupService.findMonthlyStatusSuccessByCard(year, month, cardNumber);
    }

    @Query("findYearlyTopupStatusSuccessByCard")
    @Description("Get yearly topup status success statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTopupYearStatusSuccess> findYearlyStatusSuccessByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return topupService.findYearlyStatusSuccessByCard(year, cardNumber);
    }

    @Query("findMonthlyTopupStatusFailedByCard")
    @Description("Get monthly topup status failed statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTopupMonthStatusFailed> findMonthlyStatusFailedByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return topupService.findMonthlyStatusFailedByCard(year, month, cardNumber);
    }

    @Query("findYearlyTopupStatusFailedByCard")
    @Description("Get yearly topup status failed statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseTopupYearStatusFailed> findYearlyStatusFailedByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return topupService.findYearlyStatusFailedByCard(year, cardNumber);
    }
}
