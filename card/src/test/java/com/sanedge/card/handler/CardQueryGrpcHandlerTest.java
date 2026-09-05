package com.sanedge.card.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sanedge.common.test.PanacheSessionPassthrough;

import com.sanedge.card.domain.response.CardResponse;
import com.sanedge.card.domain.response.CardResponseDeleteAt;
import com.sanedge.card.service.CardQueryService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.card.Card;
import pb.card.CardQuery;
import pb.user.User;
import pb.user.UserQueryService;

@ExtendWith({ MockitoExtension.class, PanacheSessionPassthrough.class })
class CardQueryGrpcHandlerTest {

    @Mock
    private CardQueryService cardQueryService;

    @Mock
    private UserQueryService userQueryService;

    private CardQueryGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CardQueryGrpcHandler();
        handler.cardQueryService = cardQueryService;
        handler.userQueryService = userQueryService;
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

    // findAllCard
    @Test
    @DisplayName("findAllCard - success")
    void findAllCard_Success() {
        Card.FindAllCardRequest request = Card.FindAllCardRequest.newBuilder()
                .setPage(1).setPageSize(10).build();
        CardResponse data = createCardResponse(1L);
        ApiResponsePagination<List<CardResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Cards retrieved", List.of(data), null);
        when(cardQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

        CardQuery.ApiResponsePaginationCard response = handler.findAllCard(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).getCardNumber()).isEqualTo("1234-5678-9012-3456");
    }

    @Test
    @DisplayName("findAllCard - error")
    void findAllCard_Error() {
        when(cardQueryService.findAll(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
        try {
            handler.findAllCard(Card.FindAllCardRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    // findByIdCard
    @Test
    @DisplayName("findByIdCard - success")
    void findByIdCard_Success() {
        Card.FindByIdCardRequest request = Card.FindByIdCardRequest.newBuilder().setCardId(1).build();
        CardResponse data = createCardResponse(1L);
        ApiResponse<CardResponse> apiResp = ApiResponse.success("Card found", data);
        when(cardQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

        Card.ApiResponseCard response = handler.findByIdCard(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByIdCard - error")
    void findByIdCard_Error() {
        when(cardQueryService.findById(anyLong())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByIdCard(Card.FindByIdCardRequest.newBuilder().setCardId(1).build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    // findByUserIdCard
    @Test
    @DisplayName("findByUserIdCard - success")
    void findByUserIdCard_Success() {
        Card.FindByUserIdCardRequest request = Card.FindByUserIdCardRequest.newBuilder().setUserId(100).build();
        CardResponse data = createCardResponse(1L);
        ApiResponse<CardResponse> apiResp = ApiResponse.success("Card found", data);
        when(cardQueryService.findByUserId(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

        Card.ApiResponseCard response = handler.findByUserIdCard(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getUserId()).isEqualTo(100);
    }

    // findByActiveCard
    @Test
    @DisplayName("findByActiveCard - success")
    void findByActiveCard_Success() {
        Card.FindAllCardRequest request = Card.FindAllCardRequest.newBuilder().setPage(1).build();
        CardResponseDeleteAt data = createCardDeleteAt(1L);
        ApiResponsePagination<List<CardResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Active cards", List.of(data), null);
        when(cardQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

        CardQuery.ApiResponsePaginationCardDeleteAt response = handler.findByActiveCard(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    // findByTrashedCard
    @Test
    @DisplayName("findByTrashedCard - success")
    void findByTrashedCard_Success() {
        Card.FindAllCardRequest request = Card.FindAllCardRequest.newBuilder().build();
        CardResponseDeleteAt data = createCardDeleteAt(2L);
        ApiResponsePagination<List<CardResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Trashed cards", List.of(data), null);
        when(cardQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

        CardQuery.ApiResponsePaginationCardDeleteAt response = handler.findByTrashedCard(request).await()
                .indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    // findByCardNumber
    @Test
    @DisplayName("findByCardNumber - success")
    void findByCardNumber_Success() {
        Card.FindByCardNumberRequest request = Card.FindByCardNumberRequest.newBuilder().setCardNumber("1234-5678")
                .build();
        CardResponse data = createCardResponse(1L);
        ApiResponse<CardResponse> apiResp = ApiResponse.success("Card found", data);
        when(cardQueryService.findByCardNumber(anyString())).thenReturn(Uni.createFrom().item(apiResp));

        Card.ApiResponseCard response = handler.findByCardNumber(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getCardNumber()).isEqualTo("1234-5678-9012-3456");
    }

    // findUserCardByCardNumber
    @Test
    @DisplayName("findUserCardByCardNumber - success with email")
    void findUserCardByCardNumber_Success() {
        Card.FindByCardNumberRequest request = Card.FindByCardNumberRequest.newBuilder().setCardNumber("1234-5678")
                .build();
        CardResponse data = createCardResponse(1L);
        ApiResponse<CardResponse> apiResp = ApiResponse.success("Card found", data);
        when(cardQueryService.findByCardNumber(anyString())).thenReturn(Uni.createFrom().item(apiResp));

        User.ApiResponseUser userResp = User.ApiResponseUser.newBuilder()
                .setData(User.UserResponse.newBuilder().setEmail("user@test.com").build())
                .build();
        when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResp));

        Card.CardWithEmailResponse response = handler.findUserCardByCardNumber(request).await().indefinitely();
        assertThat(response.getEmail()).isEqualTo("user@test.com");
        assertThat(response.getCardNumber()).isEqualTo("1234-5678-9012-3456");
    }

    @Test
    @DisplayName("findUserCardByCardNumber - card not found")
    void findUserCardByCardNumber_NotFound() {
        Card.FindByCardNumberRequest request = Card.FindByCardNumberRequest.newBuilder().setCardNumber("invalid")
                .build();
        when(cardQueryService.findByCardNumber(anyString())).thenReturn(Uni.createFrom().nullItem());
        try {
            handler.findUserCardByCardNumber(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    // edge cases
    @Test
    @DisplayName("findAllCard - empty list")
    void findAllCard_Empty() {
        Card.FindAllCardRequest request = Card.FindAllCardRequest.newBuilder().build();
        when(cardQueryService.findAll(any())).thenReturn(
                Uni.createFrom().item(new ApiResponsePagination<>("success", "No cards", List.of(), null)));
        CardQuery.ApiResponsePaginationCard response = handler.findAllCard(request).await().indefinitely();
        assertThat(response.getDataCount()).isZero();
    }

    @Test
    @DisplayName("findByIdCard - null data")
    void findByIdCard_NullData() {
        Card.FindByIdCardRequest request = Card.FindByIdCardRequest.newBuilder().setCardId(1).build();
        when(cardQueryService.findById(anyLong()))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("No data", null)));
        Card.ApiResponseCard response = handler.findByIdCard(request).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}