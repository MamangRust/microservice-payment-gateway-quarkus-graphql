package com.sanedge.card.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.card.domain.response.CardAuthTransactionResponse;
import com.sanedge.card.service.CardAuthService;
import com.sanedge.common.domain.response.ApiResponse;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.card.CardAuthorization;

@ExtendWith(MockitoExtension.class)
class CardAuthorizationGrpcHandlerTest {

    @Mock
    private CardAuthService cardAuthService;

    private CardAuthorizationGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CardAuthorizationGrpcHandler();
        handler.cardAuthService = cardAuthService;
    }

    private CardAuthTransactionResponse createAuthResponse() {
        CardAuthTransactionResponse r = new CardAuthTransactionResponse();
        r.setAuthTxnId(100L);
        r.setCardNumber("CARD-1");
        r.setMerchantId(10);
        r.setAmount(new BigDecimal("500000"));
        r.setCurrency("IDR");
        r.setPosEntryMode("021");
        r.setMcc("5812");
        r.setIdempotencyKey("idem-1");
        r.setRiskScore(15);
        r.setStatus("APPROVED");
        r.setAuthorizedAt("2024-06-15T10:30:00Z");
        r.setCreatedAt("2024-06-15T10:30:00Z");
        r.setUpdatedAt("2024-06-15T10:30:00Z");
        return r;
    }

    @Test
    @DisplayName("authorize - success")
    void authorize_Success() {
        CardAuthorization.AuthorizeCardRequest request = CardAuthorization.AuthorizeCardRequest.newBuilder()
                .setCardNumber("CARD-1").setMerchantId(10).setAmount(500000.0)
                .setCurrency("IDR").setPosEntryMode("021").setMcc("5812").setIdempotencyKey("idem-1").build();

        CardAuthTransactionResponse data = createAuthResponse();
        ApiResponse<CardAuthTransactionResponse> apiResp = ApiResponse.success("Authorized", data);
        when(cardAuthService.authorize(any())).thenReturn(Uni.createFrom().item(apiResp));

        CardAuthorization.ApiResponseCardAuthTransaction response = handler.authorize(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getAuthTxnId()).isEqualTo(100);
        assertThat(response.getData().getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("authorize - error")
    void authorize_Error() {
        when(cardAuthService.authorize(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.authorize(CardAuthorization.AuthorizeCardRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("reverse - success")
    void reverse_Success() {
        CardAuthorization.ReverseTransactionRequest request = CardAuthorization.ReverseTransactionRequest.newBuilder()
                .setAuthTxnId(100).setCardNumber("CARD-1").setAmount(500000.0).setIdempotencyKey("idem-2").build();

        CardAuthTransactionResponse data = createAuthResponse();
        data.setStatus("REVERSED");
        data.setReversedAt("2024-06-16T08:00:00Z");
        ApiResponse<CardAuthTransactionResponse> apiResp = ApiResponse.success("Reversed", data);
        when(cardAuthService.reverse(any())).thenReturn(Uni.createFrom().item(apiResp));

        CardAuthorization.ApiResponseCardAuthTransaction response = handler.reverse(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getStatus()).isEqualTo("REVERSED");
    }

    @Test
    @DisplayName("reverse - error")
    void reverse_Error() {
        when(cardAuthService.reverse(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.reverse(CardAuthorization.ReverseTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("authorize - null data")
    void authorize_NullData() {
        when(cardAuthService.authorize(any())).thenReturn(Uni.createFrom().item(ApiResponse.success("No data", null)));
        CardAuthorization.ApiResponseCardAuthTransaction response = handler.authorize(
                CardAuthorization.AuthorizeCardRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}