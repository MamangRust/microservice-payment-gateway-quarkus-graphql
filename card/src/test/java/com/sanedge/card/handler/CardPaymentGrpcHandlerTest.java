package com.sanedge.card.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.card.domain.response.CardPaymentResponse;
import com.sanedge.card.service.CardPaymentService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.card.CardPayment;

@ExtendWith(MockitoExtension.class)
class CardPaymentGrpcHandlerTest {

        @Mock
        private CardPaymentService cardPaymentService;

        private CardPaymentGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new CardPaymentGrpcHandler();
                handler.cardPaymentService = cardPaymentService;
        }

        private CardPaymentResponse samplePayment() {
                CardPaymentResponse r = new CardPaymentResponse();
                r.setPaymentId(1L);
                r.setCardNumber("CARD-1");
                r.setAmount(new BigDecimal("100000"));
                r.setPaymentChannel("BANK_TRANSFER");
                r.setReferenceId("REF-001");
                r.setStatus("COMPLETED");
                r.setPaidAt(Instant.now().toString());
                r.setCreatedAt(Instant.now().toString());
                r.setUpdatedAt(Instant.now().toString());
                return r;
        }

        @Test
        @DisplayName("postPayment - success")
        void postPayment_Success() {
                CardPayment.PostPaymentRequest request = CardPayment.PostPaymentRequest.newBuilder()
                                .setCardNumber("CARD-1").setAmount(100000.0).setPaymentChannel("BANK_TRANSFER")
                                .setReferenceId("REF-001").build();
                CardPaymentResponse data = samplePayment();
                ApiResponse<CardPaymentResponse> apiResp = ApiResponse.success("Payment posted", data);
                when(cardPaymentService.postPayment(any())).thenReturn(Uni.createFrom().item(apiResp));

                CardPayment.ApiResponseCardPayment response = handler.postPayment(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getAmount()).isEqualTo(100000.0);
        }

        @Test
        @DisplayName("postPayment - error")
        void postPayment_Error() {
                when(cardPaymentService.postPayment(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
                try {
                        handler.postPayment(CardPayment.PostPaymentRequest.newBuilder().build()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        @Test
        @DisplayName("getPaymentHistory - success")
        void getPaymentHistory_Success() {
                CardPayment.GetPaymentHistoryRequest request = CardPayment.GetPaymentHistoryRequest.newBuilder()
                                .setCardNumber("CARD-1").setPage(1).setPageSize(10).build();
                CardPaymentResponse data = samplePayment();
                ApiResponsePagination<List<CardPaymentResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "History", List.of(data), null);
                when(cardPaymentService.getPaymentHistory(anyString(), anyInt(), anyInt()))
                                .thenReturn(Uni.createFrom().item(apiResp));

                CardPayment.ApiResponsePaginationCardPayment response = handler.getPaymentHistory(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).getCardNumber()).isEqualTo("CARD-1");
        }

        @Test
        @DisplayName("countPayments - success")
        void countPayments_Success() {
                CardPayment.GetPaymentHistoryRequest request = CardPayment.GetPaymentHistoryRequest.newBuilder()
                                .setCardNumber("CARD-1").build();
                when(cardPaymentService.countPayments(anyString()))
                                .thenReturn(Uni.createFrom().item(new ApiResponse<>("success", "Count", 5L)));

                CardPayment.ApiResponseCountPayment response = handler.countPayments(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData()).isEqualTo(5L);
        }

        @Test
        @DisplayName("countPayments - error")
        void countPayments_Error() {
                when(cardPaymentService.countPayments(anyString()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
                try {
                        handler.countPayments(
                                        CardPayment.GetPaymentHistoryRequest.newBuilder().setCardNumber("x").build())
                                        .await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        @Test
        @DisplayName("postPayment - null data")
        void postPayment_NullData() {
                when(cardPaymentService.postPayment(any()))
                                .thenReturn(Uni.createFrom().item(ApiResponse.success("Created", null)));
                CardPayment.ApiResponseCardPayment response = handler.postPayment(
                                CardPayment.PostPaymentRequest.newBuilder().build()).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }
}