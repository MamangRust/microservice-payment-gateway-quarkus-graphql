package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.CardDto.*;
import com.sanedge.gateway.service.CardService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class CardResource {

    @Inject
    CardService cardService;

    @Query("listCards")
    @Description("List all cards")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllCardResponse> listCards(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return cardService.listCards(page, size, search);
    }

    @Query("findActiveCards")
    @Description("List active cards")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllCardResponse> findActiveCards(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return cardService.findActiveCards(page, size, search);
    }

    @Query("findTrashedCards")
    @Description("List trashed cards")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllCardResponse> findTrashedCards(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return cardService.findTrashedCards(page, size, search);
    }

    @Query("getCard")
    @Description("Get card by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdCardResponse> getCard(@Name("id") int id) {
        return cardService.getCard(id);
    }

    @Query("findCardByUser")
    @Description("Get cards by user ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdCardResponse> findCardByUser(@Name("userId") int userId) {
        return cardService.findCardByUser(userId);
    }

    @Query("findCardByNumber")
    @Description("Get card by number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdCardResponse> findCardByNumber(@Name("cardNumber") String cardNumber) {
        return cardService.findCardByNumber(cardNumber);
    }

    @Query("findUserCardByCardNumber")
    @Description("Get card with email by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<CardWithEmailResponse> findUserCardByCardNumber(@Name("cardNumber") String cardNumber) {
        return cardService.findUserCardByCardNumber(cardNumber);
    }

    @Mutation("createCard")
    @Description("Create a new card")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<CreateCardResponse> createCard(@Name("body") CreateCardRequest body) {
        return cardService.createCard(body);
    }

    @Mutation("updateCard")
    @Description("Update card")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<UpdateCardResponse> updateCard(@Name("id") int id, @Name("body") UpdateCardRequest body) {
        return cardService.updateCard(id, body);
    }

    @Mutation("deleteCardPermanent")
    @Description("Permanently delete a card")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteCardPermanent(@Name("id") int id) {
        return cardService.deleteCardPermanent(id);
    }

    @Mutation("deleteCard")
    @Description("Soft-delete a card")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedCardResponse> deleteCard(@Name("id") int id) {
        return cardService.deleteCard(id);
    }

    @Mutation("trashCard")
    @Description("Soft-delete card by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedCardResponse> trashCard(@Name("id") int id) {
        return cardService.trashCard(id);
    }

    @Mutation("restoreCard")
    @Description("Restore card by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedCardResponse> restoreCard(@Name("id") int id) {
        return cardService.restoreCard(id);
    }

    @Mutation("restoreAllCards")
    @Description("Restore all cards")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> restoreAllCards() {
        return cardService.restoreAllCards();
    }

    @Mutation("deleteAllCards")
    @Description("Delete all cards permanently")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteAllCards() {
        return cardService.deleteAllCards();
    }

    @Query("findMonthlyBalance")
    @Description("Get monthly balance statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMonthlyBalance> findMonthlyBalance(@Name("year") int year) {
        return cardService.findMonthlyBalance(year);
    }

    @Query("findYearlyBalance")
    @Description("Get yearly balance statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseYearlyBalance> findYearlyBalance(@Name("year") int year) {
        return cardService.findYearlyBalance(year);
    }

    @Query("getMonthlyBalanceByCard")
    @Description("Get monthly balance statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMonthlyBalance> getMonthlyBalanceByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getMonthlyBalanceByCard(year, cardNumber);
    }

    @Query("getYearlyBalanceByCard")
    @Description("Get yearly balance statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseYearlyBalance> getYearlyBalanceByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getYearlyBalanceByCard(year, cardNumber);
    }

    @Query("findMonthlyTopupAmount")
    @Description("Get monthly topup statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMonthlyAmount> findMonthlyTopupAmount(@Name("year") int year) {
        return cardService.findMonthlyTopupAmount(year);
    }

    @Query("findYearlyTopupAmount")
    @Description("Get yearly topup statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseYearlyAmount> findYearlyTopupAmount(@Name("year") int year) {
        return cardService.findYearlyTopupAmount(year);
    }

    @Query("getMonthlyTopupAmountByCard")
    @Description("Get monthly topup statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMonthlyAmount> getMonthlyTopupAmountByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getMonthlyTopupAmountByCard(year, cardNumber);
    }

    @Query("getYearlyTopupAmountByCard")
    @Description("Get yearly topup statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseYearlyAmount> getYearlyTopupAmountByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getYearlyTopupAmountByCard(year, cardNumber);
    }

    @Query("findMonthlyTransactionAmount")
    @Description("Get monthly transaction statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransactionAmount(@Name("year") int year) {
        return cardService.findMonthlyTransactionAmount(year);
    }

    @Query("findYearlyTransactionAmount")
    @Description("Get yearly transaction statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseYearlyAmount> findYearlyTransactionAmount(@Name("year") int year) {
        return cardService.findYearlyTransactionAmount(year);
    }

    @Query("getMonthlyTransactionAmountByCard")
    @Description("Get monthly transaction statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMonthlyAmount> getMonthlyTransactionAmountByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getMonthlyTransactionAmountByCard(year, cardNumber);
    }

    @Query("getYearlyTransactionAmountByCard")
    @Description("Get yearly transaction statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseYearlyAmount> getYearlyTransactionAmountByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getYearlyTransactionAmountByCard(year, cardNumber);
    }

    @Query("findMonthlyTransferAmountSender")
    @Description("Get monthly transfer sender statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransferAmountSender(@Name("year") int year) {
        return cardService.findMonthlyTransferAmountSender(year);
    }

    @Query("findMonthlyTransferAmountReceiver")
    @Description("Get monthly transfer receiver statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransferAmountReceiver(@Name("year") int year) {
        return cardService.findMonthlyTransferAmountReceiver(year);
    }

    @Query("findYearlyTransferAmountSender")
    @Description("Get yearly transfer sender statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseYearlyAmount> findYearlyTransferAmountSender(@Name("year") int year) {
        return cardService.findYearlyTransferAmountSender(year);
    }

    @Query("findYearlyTransferAmountReceiver")
    @Description("Get yearly transfer receiver statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseYearlyAmount> findYearlyTransferAmountReceiver(@Name("year") int year) {
        return cardService.findYearlyTransferAmountReceiver(year);
    }

    @Query("getMonthlyTransferAmountByCardSender")
    @Description("Get monthly transfer sender statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMonthlyAmount> getMonthlyTransferAmountByCardSender(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getMonthlyTransferAmountByCardSender(year, cardNumber);
    }

    @Query("getMonthlyTransferAmountByCardReceiver")
    @Description("Get monthly transfer receiver statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMonthlyAmount> getMonthlyTransferAmountByCardReceiver(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getMonthlyTransferAmountByCardReceiver(year, cardNumber);
    }

    @Query("getYearlyTransferAmountByCardSender")
    @Description("Get yearly transfer sender statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseYearlyAmount> getYearlyTransferAmountByCardSender(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getYearlyTransferAmountByCardSender(year, cardNumber);
    }

    @Query("getYearlyTransferAmountByCardReceiver")
    @Description("Get yearly transfer receiver statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseYearlyAmount> getYearlyTransferAmountByCardReceiver(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getYearlyTransferAmountByCardReceiver(year, cardNumber);
    }

    @Query("findMonthlyWithdrawAmount")
    @Description("Get monthly withdraw statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseMonthlyAmount> findMonthlyWithdrawAmount(@Name("year") int year) {
        return cardService.findMonthlyWithdrawAmount(year);
    }

    @Query("findYearlyWithdrawAmount")
    @Description("Get yearly withdraw statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseYearlyAmount> findYearlyWithdrawAmount(@Name("year") int year) {
        return cardService.findYearlyWithdrawAmount(year);
    }

    @Query("getMonthlyWithdrawAmountByCard")
    @Description("Get monthly withdraw statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseMonthlyAmount> getMonthlyWithdrawAmountByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getMonthlyWithdrawAmountByCard(year, cardNumber);
    }

    @Query("getYearlyWithdrawAmountByCard")
    @Description("Get yearly withdraw statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseYearlyAmount> getYearlyWithdrawAmountByCard(
            @Name("year") int year,
            @Name("cardNumber") String cardNumber) {
        return cardService.getYearlyWithdrawAmountByCard(year, cardNumber);
    }

    @Query("findCardDashboard")
    @Description("Get card dashboard statistics")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<ApiResponseDashboardCard> findCardDashboard() {
        return cardService.findCardDashboard();
    }

    @Query("findCardDashboardByCardNumber")
    @Description("Get card dashboard statistics by card number")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<ApiResponseDashboardCardNumber> findCardDashboardByCardNumber(@Name("cardNumber") String cardNumber) {
        return cardService.findCardDashboardByCardNumber(cardNumber);
    }
}
