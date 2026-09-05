package com.sanedge.card.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.sanedge.card.domain.response.CardResponse;
import com.sanedge.card.domain.response.CardResponseDeleteAt;
import com.sanedge.card.service.CardCommandService;
import com.sanedge.common.domain.response.ApiResponse;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.card.Card;
import pb.card.CardCommand;

@ExtendWith(MockitoExtension.class)
class CardCommandGrpcHandlerTest {

        @Mock
        private CardCommandService cardCommandService;

        private CardCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new CardCommandGrpcHandler();
                handler.cardCommandService = cardCommandService;
        }

        // helpers
        private CardResponse createCardResponse(Long id) {
                CardResponse r = new CardResponse();
                r.setId(id);
                r.setCardNumber("1234-5678-9012-3456");
                r.setCardType("VISA");
                r.setCardProvider("BCA");
                r.setUserId(100L);
                r.setExpireDate(LocalDate.of(2027, 12, 31).toString());
                r.setCvv("123");
                r.setCreatedAt("2024-01-01T00:00:00Z");
                r.setUpdatedAt("2024-01-02T00:00:00Z");
                return r;
        }

        private CardResponseDeleteAt createCardDeleteAt(Long id) {
                CardResponseDeleteAt r = new CardResponseDeleteAt();
                r.setId(id);
                r.setCardNumber("1234-5678-9012-3456");
                r.setCardType("VISA");
                r.setCardProvider("BCA");
                r.setUserId(100L);
                r.setExpireDate(LocalDate.of(2027, 12, 31).toString());
                r.setCvv("123");
                r.setCreatedAt("2024-01-01T00:00:00Z");
                r.setUpdatedAt("2024-01-02T00:00:00Z");
                r.setDeletedAt("2024-06-01T00:00:00Z");
                return r;
        }

        @Test
        @DisplayName("createCard - success")
        void createCard_Success() {
                CardCommand.CreateCardRequest request = CardCommand.CreateCardRequest.newBuilder()
                                .setUserId(100).setCardType("VISA").setCvv("123").setCardProvider("BCA").build();
                CardResponse data = createCardResponse(1L);
                ApiResponse<CardResponse> apiResp = ApiResponse.success("Created", data);
                when(cardCommandService.createCard(any())).thenReturn(Uni.createFrom().item(apiResp));

                Card.ApiResponseCard response = handler.createCard(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("createCard - error")
        void createCard_Error() {
                when(cardCommandService.createCard(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
                try {
                        handler.createCard(CardCommand.CreateCardRequest.newBuilder().build()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        @Test
        @DisplayName("updateCard - success")
        void updateCard_Success() {
                CardCommand.UpdateCardRequest request = CardCommand.UpdateCardRequest.newBuilder()
                                .setCardId(1).setUserId(100).setCardType("MASTERCARD").build();
                CardResponse data = createCardResponse(1L);
                data.setCardType("MASTERCARD");
                ApiResponse<CardResponse> apiResp = ApiResponse.success("Updated", data);
                when(cardCommandService.updateCard(any())).thenReturn(Uni.createFrom().item(apiResp));

                Card.ApiResponseCard response = handler.updateCard(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getCardType()).isEqualTo("MASTERCARD");
        }

        @Test
        @DisplayName("trashedCard - success")
        void trashedCard_Success() {
                Card.FindByIdCardRequest request = Card.FindByIdCardRequest.newBuilder().setCardId(1).build();
                CardResponseDeleteAt data = createCardDeleteAt(1L);
                ApiResponse<CardResponseDeleteAt> apiResp = ApiResponse.success("Trashed", data);
                when(cardCommandService.trashCard(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Card.ApiResponseCardDeleteAt response = handler.trashedCard(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("restoreCard - success")
        void restoreCard_Success() {
                Card.FindByIdCardRequest request = Card.FindByIdCardRequest.newBuilder().setCardId(1).build();
                CardResponseDeleteAt data = createCardDeleteAt(1L);
                data.setDeletedAt(null); // restored
                ApiResponse<CardResponseDeleteAt> apiResp = ApiResponse.success("Restored", data);
                when(cardCommandService.restoreCard(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Card.ApiResponseCardDeleteAt response = handler.restoreCard(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isFalse();
        }

        @Test
        @DisplayName("deleteCardPermanent - success")
        void deleteCardPermanent_Success() {
                Card.FindByIdCardRequest request = Card.FindByIdCardRequest.newBuilder().setCardId(1).build();
                ApiResponse<Boolean> apiResp = ApiResponse.success("Permanently deleted", true);
                when(cardCommandService.deleteCard(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                CardCommand.ApiResponseCardDelete response = handler.deleteCardPermanent(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Permanently deleted");
        }

        @Test
        @DisplayName("restoreAllCard - success")
        void restoreAllCard_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All restored", true);
                when(cardCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

                CardCommand.ApiResponseCardAll response = handler.restoreAllCard(Empty.getDefaultInstance()).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All restored");
        }

        @Test
        @DisplayName("deleteAllCardPermanent - success")
        void deleteAllCard_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All permanently deleted", true);
                when(cardCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

                CardCommand.ApiResponseCardAll response = handler.deleteAllCardPermanent(Empty.getDefaultInstance())
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All permanently deleted");
        }

        // edge cases
        @Test
        @DisplayName("createCard - null data")
        void createCard_NullData() {
                when(cardCommandService.createCard(any()))
                                .thenReturn(Uni.createFrom().item(ApiResponse.success("Created", null)));
                Card.ApiResponseCard response = handler.createCard(CardCommand.CreateCardRequest.newBuilder().build())
                                .await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("updateCard - error")
        void updateCard_Error() {
                when(cardCommandService.updateCard(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
                try {
                        handler.updateCard(CardCommand.UpdateCardRequest.newBuilder().build()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }
}