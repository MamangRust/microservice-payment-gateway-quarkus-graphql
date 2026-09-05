package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.CardDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.card.MutinyCardQueryServiceGrpc.MutinyCardQueryServiceStub cardQueryService;

    @Mock
    pb.card.MutinyCardCommandServiceGrpc.MutinyCardCommandServiceStub cardCommandService;

    @Mock
    pb.card.MutinyCardDashboardServiceGrpc.MutinyCardDashboardServiceStub cardDashboardService;

    @Mock
    pb.card.stats.MutinyCardStatsBalanceServiceGrpc.MutinyCardStatsBalanceServiceStub cardStatsBalanceService;

    @Mock
    pb.card.stats.MutinyCardStatsTopupServiceGrpc.MutinyCardStatsTopupServiceStub cardStatsTopupService;

    @Mock
    pb.card.stats.MutinyCardStatsTransactionServiceGrpc.MutinyCardStatsTransactionServiceStub cardStatsTransactionService;

    @Mock
    pb.card.stats.MutinyCardStatsTransferServiceGrpc.MutinyCardStatsTransferServiceStub cardStatsTransferService;

    @Mock
    pb.card.stats.MutinyCardStatsWithdrawServiceGrpc.MutinyCardStatsWithdrawServiceStub cardStatsWithdrawService;

    CardServiceImpl cardService;

    @BeforeEach
    void setUp() throws Exception {
        cardService = new CardServiceImpl();

        setField(cardService, "telemetryHelper", telemetryHelper);
        setField(cardService, "cardQueryService", cardQueryService);
        setField(cardService, "cardCommandService", cardCommandService);
        setField(cardService, "cardDashboardService", cardDashboardService);
        setField(cardService, "cardStatsBalanceService", cardStatsBalanceService);
        setField(cardService, "cardStatsTopupService", cardStatsTopupService);
        setField(cardService, "cardStatsTransactionService", cardStatsTransactionService);
        setField(cardService, "cardStatsTransferService", cardStatsTransferService);
        setField(cardService, "cardStatsWithdrawService", cardStatsWithdrawService);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(1);
                    return supplier.get();
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void listCards_returnsResponse() {
        pb.card.Card.CardResponse protoCard = pb.card.Card.CardResponse.newBuilder()
                .setId(1)
                .setUserId(100)
                .setCardNumber("4111111111111111")
                .setCardType("DEBIT")
                .setExpireDate("2027-12-31T23:59:59Z")
                .setCvv("123")
                .setCardProvider("VISA")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.card.CardQuery.ApiResponsePaginationCard paginationResponse = pb.card.CardQuery.ApiResponsePaginationCard.newBuilder()
                .addData(protoCard)
                .setStatus("success")
                .setMessage("Cards found")
                .build();

        when(cardQueryService.findAllCard(any(pb.card.Card.FindAllCardRequest.class)))
                .thenReturn(Uni.createFrom().item(paginationResponse));

        FindAllCardResponse result = cardService.listCards(1, 10, "")
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).cardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void getCard_returnsCard() {
        pb.card.Card.CardResponse protoCard = pb.card.Card.CardResponse.newBuilder()
                .setId(1)
                .setUserId(100)
                .setCardNumber("4111111111111111")
                .setCardType("DEBIT")
                .setExpireDate("2027-12-31T23:59:59Z")
                .setCvv("123")
                .setCardProvider("VISA")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.card.Card.ApiResponseCard apiResponse = pb.card.Card.ApiResponseCard.newBuilder()
                .setData(protoCard)
                .setStatus("success")
                .setMessage("Card found")
                .build();

        when(cardQueryService.findByIdCard(any(pb.card.Card.FindByIdCardRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        FindByIdCardResponse result = cardService.getCard(1)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
        assertThat(result.data().cardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void getCard_returnsNullData_whenNotFound() {
        pb.card.Card.ApiResponseCard apiResponse = pb.card.Card.ApiResponseCard.newBuilder()
                .setStatus("error")
                .setMessage("Card not found")
                .build();

        when(cardQueryService.findByIdCard(any(pb.card.Card.FindByIdCardRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        FindByIdCardResponse result = cardService.getCard(999)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("error");
        assertThat(result.data()).isNull();
    }

    @Test
    void createCard_returnsCreatedCard() {
        pb.card.Card.CardResponse protoCard = pb.card.Card.CardResponse.newBuilder()
                .setId(1)
                .setUserId(100)
                .setCardNumber("4111111111111111")
                .setCardType("DEBIT")
                .setExpireDate("2027-12-31T23:59:59Z")
                .setCvv("123")
                .setCardProvider("VISA")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.card.Card.ApiResponseCard apiResponse = pb.card.Card.ApiResponseCard.newBuilder()
                .setData(protoCard)
                .setStatus("success")
                .setMessage("Card created")
                .build();

        when(cardCommandService.createCard(any(pb.card.CardCommand.CreateCardRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        CreateCardRequest request = new CreateCardRequest(
                100,
                "DEBIT",
                "2027-12-31T23:59:59Z",
                "123",
                "VISA"
        );

        CreateCardResponse result = cardService.createCard(request)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().userId()).isEqualTo(100);
    }

    @Test
    void deleteCard_returnsTrashedCard() {
        pb.card.Card.CardResponseDeleteAt protoCard = pb.card.Card.CardResponseDeleteAt.newBuilder()
                .setId(1)
                .setUserId(100)
                .setCardNumber("4111111111111111")
                .setCardType("DEBIT")
                .setExpireDate("2027-12-31T23:59:59Z")
                .setCvv("123")
                .setCardProvider("VISA")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.card.Card.ApiResponseCardDeleteAt apiResponse = pb.card.Card.ApiResponseCardDeleteAt.newBuilder()
                .setData(protoCard)
                .setStatus("success")
                .setMessage("Card trashed")
                .build();

        when(cardCommandService.trashedCard(any(pb.card.Card.FindByIdCardRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        TrashedCardResponse result = cardService.deleteCard(1)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void restoreCard_returnsRestoredCard() {
        pb.card.Card.CardResponseDeleteAt protoCard = pb.card.Card.CardResponseDeleteAt.newBuilder()
                .setId(1)
                .setUserId(100)
                .setCardNumber("4111111111111111")
                .setCardType("DEBIT")
                .setExpireDate("2027-12-31T23:59:59Z")
                .setCvv("123")
                .setCardProvider("VISA")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.card.Card.ApiResponseCardDeleteAt apiResponse = pb.card.Card.ApiResponseCardDeleteAt.newBuilder()
                .setData(protoCard)
                .setStatus("success")
                .setMessage("Card restored")
                .build();

        when(cardCommandService.restoreCard(any(pb.card.Card.FindByIdCardRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        TrashedCardResponse result = cardService.restoreCard(1)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void deleteCardPermanent_returnsStatus() {
        pb.card.CardCommand.ApiResponseCardDelete apiResponse = pb.card.CardCommand.ApiResponseCardDelete.newBuilder()
                .setStatus("success")
                .setMessage("Card permanently deleted")
                .build();

        when(cardCommandService.deleteCardPermanent(any(pb.card.Card.FindByIdCardRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        SimpleStatusMessageResponse result = cardService.deleteCardPermanent(1)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Card permanently deleted");
    }

    @Test
    void restoreAllCards_returnsStatus() {
        pb.card.CardCommand.ApiResponseCardAll apiResponse = pb.card.CardCommand.ApiResponseCardAll.newBuilder()
                .setStatus("success")
                .setMessage("All cards restored")
                .build();

        when(cardCommandService.restoreAllCard(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        SimpleStatusMessageResponse result = cardService.restoreAllCards()
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void findCardByNumber_returnsCard() {
        pb.card.Card.CardResponse protoCard = pb.card.Card.CardResponse.newBuilder()
                .setId(1)
                .setUserId(100)
                .setCardNumber("4111111111111111")
                .setCardType("DEBIT")
                .setExpireDate("2027-12-31T23:59:59Z")
                .setCvv("123")
                .setCardProvider("VISA")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.card.Card.ApiResponseCard apiResponse = pb.card.Card.ApiResponseCard.newBuilder()
                .setData(protoCard)
                .setStatus("success")
                .setMessage("Card found")
                .build();

        when(cardQueryService.findByCardNumber(any(pb.card.Card.FindByCardNumberRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        FindByIdCardResponse result = cardService.findCardByNumber("4111111111111111")
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void findCardByUser_returnsCard() {
        pb.card.Card.CardResponse protoCard = pb.card.Card.CardResponse.newBuilder()
                .setId(1)
                .setUserId(100)
                .setCardNumber("4111111111111111")
                .setCardType("DEBIT")
                .setExpireDate("2027-12-31T23:59:59Z")
                .setCvv("123")
                .setCardProvider("VISA")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.card.Card.ApiResponseCard apiResponse = pb.card.Card.ApiResponseCard.newBuilder()
                .setData(protoCard)
                .setStatus("success")
                .setMessage("Card found")
                .build();

        when(cardQueryService.findByUserIdCard(any(pb.card.Card.FindByUserIdCardRequest.class)))
                .thenReturn(Uni.createFrom().item(apiResponse));

        FindByIdCardResponse result = cardService.findCardByUser(100)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().userId()).isEqualTo(100);
    }
}