package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.WithdrawDto.*;
import com.sanedge.gateway.service.WithdrawService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class WithdrawResource {

    @Inject
    WithdrawService withdrawService;

    @Query("listWithdraws")
    @Description("List all withdraws")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllWithdrawResponse> listWithdraws(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return withdrawService.listWithdraws(page, size, search);
    }

    @Query("findWithdrawByCard")
    @Description("Get withdraws by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindAllWithdrawResponse> findByCard(@Name("cardNumber") String cardNumber) {
        return withdrawService.findByCard(cardNumber);
    }

    @Query("findActiveWithdraws")
    @Description("List active withdraws")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllWithdrawResponse> findActiveWithdraws(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return withdrawService.findActiveWithdraws(page, size, search);
    }

    @Query("findTrashedWithdraws")
    @Description("List trashed withdraws")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllWithdrawResponse> findTrashedWithdraws(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return withdrawService.findTrashedWithdraws(page, size, search);
    }

    @Query("getWithdraw")
    @Description("Get withdraw by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdWithdrawResponse> getWithdraw(@Name("id") int id) {
        return withdrawService.getWithdraw(id);
    }

    @Mutation("createWithdraw")
    @Description("Create a new withdraw")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<CreateWithdrawResponse> createWithdraw(@Name("body") CreateWithdrawBody body) {
        return withdrawService.createWithdraw(body);
    }

    @Mutation("updateWithdraw")
    @Description("Update withdraw")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<UpdateWithdrawResponse> updateWithdraw(@Name("id") int id, @Name("body") CreateWithdrawBody body) {
        return withdrawService.updateWithdraw(id, body);
    }

    @Mutation("deleteWithdraw")
    @Description("Soft-delete a withdraw")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<TrashedWithdrawResponse> deleteWithdraw(@Name("id") int id) {
        return withdrawService.deleteWithdraw(id);
    }

    @Mutation("deleteWithdrawPermanent")
    @Description("Permanently delete a withdraw")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteWithdrawPermanent(@Name("id") int id) {
        return withdrawService.deleteWithdrawPermanent(id);
    }

    @Mutation("trashWithdraw")
    @Description("Soft-delete withdraw by ID")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<TrashedWithdrawResponse> trashWithdraw(@Name("id") int id) {
        return withdrawService.trashWithdraw(id);
    }

    @Mutation("restoreWithdraw")
    @Description("Restore withdraw by ID")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<TrashedWithdrawResponse> restoreWithdraw(@Name("id") int id) {
        return withdrawService.restoreWithdraw(id);
    }

    @Mutation("restoreAllWithdraws")
    @Description("Restore all withdraws")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> restoreAllWithdraws() {
        return withdrawService.restoreAllWithdraws();
    }

    @Mutation("deleteAllWithdraws")
    @Description("Delete all withdraws permanently")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteAllWithdraws() {
        return withdrawService.deleteAllWithdraws();
    }

    @Query("findMonthlyWithdrawAmounts")
    @Description("Get monthly withdraw amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseWithdrawMonthAmount> findMonthlyAmounts(@Name("year") int year) {
        return withdrawService.findMonthlyAmounts(year);
    }

    @Query("findYearlyWithdrawAmounts")
    @Description("Get yearly withdraw amount statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseWithdrawYearAmount> findYearlyAmounts(@Name("year") int year) {
        return withdrawService.findYearlyAmounts(year);
    }

    @Query("findMonthlyWithdrawByCard")
    @Description("Get monthly withdraw amount statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseWithdrawMonthAmount> findMonthlyByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return withdrawService.findMonthlyByCard(year, cardNumber);
    }

    @Query("findYearlyWithdrawByCard")
    @Description("Get yearly withdraw amount statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseWithdrawYearAmount> findYearlyByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return withdrawService.findYearlyByCard(year, cardNumber);
    }

    @Query("findMonthlyWithdrawStatusSuccess")
    @Description("Get monthly withdraw status success statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseWithdrawMonthStatusSuccess> findMonthlyStatusSuccess(
            @Name("year") int year,
            @Name("month") int month) {
        return withdrawService.findMonthlyStatusSuccess(year, month);
    }

    @Query("findYearlyWithdrawStatusSuccess")
    @Description("Get yearly withdraw status success statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseWithdrawYearStatusSuccess> findYearlyStatusSuccess(@Name("year") int year) {
        return withdrawService.findYearlyStatusSuccess(year);
    }

    @Query("findMonthlyWithdrawStatusFailed")
    @Description("Get monthly withdraw status failed statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseWithdrawMonthStatusFailed> findMonthlyStatusFailed(
            @Name("year") int year,
            @Name("month") int month) {
        return withdrawService.findMonthlyStatusFailed(year, month);
    }

    @Query("findYearlyWithdrawStatusFailed")
    @Description("Get yearly withdraw status failed statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseWithdrawYearStatusFailed> findYearlyStatusFailed(@Name("year") int year) {
        return withdrawService.findYearlyStatusFailed(year);
    }

    @Query("findMonthlyWithdrawStatusSuccessByCard")
    @Description("Get monthly withdraw status success statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseWithdrawMonthStatusSuccess> findMonthlyStatusSuccessByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return withdrawService.findMonthlyStatusSuccessByCard(year, month, cardNumber);
    }

    @Query("findYearlyWithdrawStatusSuccessByCard")
    @Description("Get yearly withdraw status success statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseWithdrawYearStatusSuccess> findYearlyStatusSuccessByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return withdrawService.findYearlyStatusSuccessByCard(year, cardNumber);
    }

    @Query("findMonthlyWithdrawStatusFailedByCard")
    @Description("Get monthly withdraw status failed statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseWithdrawMonthStatusFailed> findMonthlyStatusFailedByCard(
            @Name("year") int year,
            @Name("month") int month,
            @Name("cardNumber") String cardNumber) {
        return withdrawService.findMonthlyStatusFailedByCard(year, month, cardNumber);
    }

    @Query("findYearlyWithdrawStatusFailedByCard")
    @Description("Get yearly withdraw status failed statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseWithdrawYearStatusFailed> findYearlyStatusFailedByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return withdrawService.findYearlyStatusFailedByCard(year, cardNumber);
    }
}
