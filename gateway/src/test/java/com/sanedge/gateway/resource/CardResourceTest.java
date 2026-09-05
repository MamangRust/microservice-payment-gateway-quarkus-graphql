package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.CardDto;
import com.sanedge.gateway.service.CardService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CardResourceTest {

    @Mock private CardService cardService;
    private CardResource cardResource;

    @BeforeEach
    void setUp() throws Exception {
        cardResource = new CardResource();
        Field f = CardResource.class.getDeclaredField("cardService");
        f.setAccessible(true);
        f.set(cardResource, cardService);
    }

    @Test
    void listCards_Success() {
        CardDto.FindAllCardResponse dto = new CardDto.FindAllCardResponse(List.of(), "success", "ok");
        lenient().when(cardService.listCards(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        CardDto.FindAllCardResponse result = cardResource.listCards(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getCard_Success() {
        CardDto.FindByIdCardResponse dto = new CardDto.FindByIdCardResponse(null, "success", "ok");
        lenient().when(cardService.getCard(anyInt())).thenReturn(Uni.createFrom().item(dto));
        CardDto.FindByIdCardResponse result = cardResource.getCard(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createCard_Success() {
        CardDto.CreateCardResponse dto = new CardDto.CreateCardResponse(null, "success", "created");
        lenient().when(cardService.createCard(any())).thenReturn(Uni.createFrom().item(dto));
        CardDto.CreateCardResponse result = cardResource.createCard(new CardDto.CreateCardRequest(1, "VISA", "2025-12-31", "123", "BCA")).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void deleteCard_Success() {
        CardDto.TrashedCardResponse dto = new CardDto.TrashedCardResponse(null, "success", "trashed");
        lenient().when(cardService.deleteCard(anyInt())).thenReturn(Uni.createFrom().item(dto));
        CardDto.TrashedCardResponse result = cardResource.deleteCard(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }

    @Test
    void findCardDashboard_Success() {
        CardDto.ApiResponseDashboardCard dto = new CardDto.ApiResponseDashboardCard("success", "ok", null);
        lenient().when(cardService.findCardDashboard()).thenReturn(Uni.createFrom().item(dto));
        CardDto.ApiResponseDashboardCard result = cardResource.findCardDashboard().await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
