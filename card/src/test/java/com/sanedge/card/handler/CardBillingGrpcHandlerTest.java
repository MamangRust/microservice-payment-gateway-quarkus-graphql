package com.sanedge.card.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.card.domain.response.BillingStatementResponse;
import com.sanedge.card.service.BillingEngineService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.card.CardBilling;

@ExtendWith(MockitoExtension.class)
class CardBillingGrpcHandlerTest {

        @Mock
        private BillingEngineService billingEngineService;

        private CardBillingGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new CardBillingGrpcHandler();
                handler.billingEngineService = billingEngineService;
        }

        private BillingStatementResponse sampleStatement() {
                BillingStatementResponse r = new BillingStatementResponse();
                r.setStatementId(1L);
                r.setCardNumber("CARD-1");
                r.setBillingCycleDay(15);
                r.setOpeningBalance(new BigDecimal("1000000"));
                r.setClosingBalance(new BigDecimal("500000"));
                r.setMinimumPayment(new BigDecimal("50000"));
                r.setDueDate("2024-07-15");
                r.setFees(BigDecimal.ZERO);
                r.setInterest(new BigDecimal("5000"));
                r.setStatementDate("2024-06-15");
                r.setStatus("CLOSED");
                r.setCreatedAt("2024-06-01T00:00:00Z");
                r.setUpdatedAt("2024-06-01T00:00:00Z");
                return r;
        }

        @Test
        @DisplayName("triggerBillingCycle - success")
        void triggerBillingCycle_Success() {
                when(billingEngineService.triggerBillingCycle(anyInt()))
                                .thenReturn(Uni.createFrom()
                                                .item(new ApiResponse<>("success", "Processed 5 statements", 5)));

                CardBilling.ApiResponseBillingStatement response = handler.triggerBillingCycle(
                                CardBilling.TriggerBillingCycleRequest.newBuilder().setBillingCycleDay(15).build())
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Processed 5 statements");
        }

        @Test
        @DisplayName("getStatement - success")
        void getStatement_Success() {
                BillingStatementResponse data = sampleStatement();
                ApiResponse<BillingStatementResponse> apiResp = ApiResponse.success("Statement found", data);
                when(billingEngineService.getStatement(anyString(), any()))
                                .thenReturn(Uni.createFrom().item(apiResp));

                CardBilling.ApiResponseBillingStatement response = handler.getStatement(
                                CardBilling.GetStatementRequest.newBuilder()
                                                .setCardNumber("CARD-1")
                                                .setStatementDate(com.google.protobuf.Timestamp.newBuilder()
                                                                .setSeconds(1718236800).build())
                                                .build())
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getClosingBalance()).isEqualTo(500000.0);
        }

        @Test
        @DisplayName("getStatementsByCard - success")
        void getStatementsByCard_Success() {
                BillingStatementResponse data = sampleStatement();
                ApiResponsePagination<List<BillingStatementResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Statements retrieved", List.of(data), null);
                when(billingEngineService.getStatementsByCard(anyString(), anyInt(), anyInt()))
                                .thenReturn(Uni.createFrom().item(apiResp));

                CardBilling.ApiResponsePaginationBillingStatement response = handler.getStatementsByCard(
                                CardBilling.GetStatementsByCardRequest.newBuilder()
                                                .setCardNumber("CARD-1").setPage(1).setPageSize(10).build())
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("getStatementsByCard - error")
        void getStatementsByCard_Error() {
                when(billingEngineService.getStatementsByCard(anyString(), anyInt(), anyInt()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.getStatementsByCard(CardBilling.GetStatementsByCardRequest.newBuilder()
                                        .setCardNumber("x").setPage(1).setPageSize(10).build()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }
}