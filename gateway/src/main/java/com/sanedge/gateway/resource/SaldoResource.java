package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.SaldoDto.*;
import com.sanedge.gateway.service.SaldoService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class SaldoResource {

    @Inject
    SaldoService saldoService;

    @Query("listSaldos")
    @Description("List all saldos")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllSaldoResponse> listSaldos(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return saldoService.listSaldos(page, size, search);
    }

    @Query("getSaldo")
    @Description("Get saldo by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdSaldoResponse> getSaldo(@Name("id") int id) {
        return saldoService.getSaldo(id);
    }

    @Query("findSaldoByCard")
    @Description("Get saldo by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdSaldoResponse> findSaldoByCard(@Name("cardNumber") String cardNumber) {
        return saldoService.findSaldoByCard(cardNumber);
    }

    @Query("findActiveSaldos")
    @Description("List active saldos")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllSaldoResponse> findActiveSaldos(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return saldoService.findActiveSaldos(page, size, search);
    }

    @Query("findTrashedSaldos")
    @Description("List trashed saldos")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllSaldoResponse> findTrashedSaldos(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return saldoService.findTrashedSaldos(page, size, search);
    }

    @Mutation("createSaldo")
    @Description("Create a new saldo")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<CreateSaldoResponse> createSaldo(@Name("body") CreateSaldoRequest body) {
        return saldoService.createSaldo(body);
    }

    @Mutation("updateSaldo")
    @Description("Update saldo")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<UpdateSaldoResponse> updateSaldo(@Name("id") int id, @Name("body") UpdateSaldoRequest body) {
        return saldoService.updateSaldo(id, body);
    }

    @Mutation("updateSaldoBalance")
    @Description("Update saldo balance")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<UpdateSaldoResponse> updateSaldoBalance(@Name("body") UpdateSaldoBalanceRequest body) {
        return saldoService.updateSaldoBalance(body);
    }

    @Mutation("updateSaldoWithdraw")
    @Description("Update saldo withdraw details")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<UpdateSaldoResponse> updateSaldoWithdraw(@Name("body") UpdateSaldoWithdrawRequest body) {
        return saldoService.updateSaldoWithdraw(body);
    }

    @Mutation("deleteSaldo")
    @Description("Soft-delete a saldo")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedSaldoResponse> deleteSaldo(@Name("id") int id) {
        return saldoService.deleteSaldo(id);
    }

    @Mutation("restoreSaldo")
    @Description("Restore a soft-deleted saldo")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedSaldoResponse> restoreSaldo(@Name("id") int id) {
        return saldoService.restoreSaldo(id);
    }

    @Mutation("deleteSaldoPermanent")
    @Description("Permanently delete a saldo")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteSaldoPermanent(@Name("id") int id) {
        return saldoService.deleteSaldoPermanent(id);
    }

    @Mutation("restoreAllSaldos")
    @Description("Restore all soft-deleted saldos")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> restoreAllSaldos() {
        return saldoService.restoreAllSaldos();
    }

    @Mutation("deleteAllSaldos")
    @Description("Delete all saldos permanently")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteAllSaldos() {
        return saldoService.deleteAllSaldos();
    }

    @Query("findMonthlySaldoBalances")
    @Description("Get monthly saldo balance statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMonthSaldoBalances> findMonthlySaldoBalances(@Name("year") int year) {
        return saldoService.findMonthlySaldoBalances(year);
    }

    @Query("findYearlySaldoBalances")
    @Description("Get yearly saldo balance statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseYearSaldoBalances> findYearlySaldoBalances(@Name("year") int year) {
        return saldoService.findYearlySaldoBalances(year);
    }

    @Query("findMonthlyTotalSaldoBalance")
    @Description("Get monthly total saldo balance statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMonthTotalSaldo> findMonthlyTotalSaldoBalance(
            @Name("year") int year,
            @Name("month") int month) {
        return saldoService.findMonthlyTotalSaldoBalance(year, month);
    }

    @Query("findYearTotalSaldoBalance")
    @Description("Get yearly total saldo balance statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseYearTotalSaldo> findYearTotalSaldoBalance(@Name("year") int year) {
        return saldoService.findYearTotalSaldoBalance(year);
    }
}
