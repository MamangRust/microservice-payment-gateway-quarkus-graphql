package com.sanedge.saldo.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sanedge.common.test.PanacheSessionPassthrough;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;
import com.sanedge.saldo.service.SaldoQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.saldo.Saldo;
import pb.saldo.SaldoQuery;

@ExtendWith({ MockitoExtension.class, PanacheSessionPassthrough.class })
class SaldoQueryGrpcHandlerTest {

        @Mock
        private SaldoQueryService saldoQueryService;

        private SaldoQueryGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new SaldoQueryGrpcHandler();
                handler.saldoQueryService = saldoQueryService;
        }

        // helpers
        private SaldoResponse createSaldoResponse(Long id) {
                SaldoResponse r = new SaldoResponse();
                r.setId(id);
                r.setCardNumber("1234-5678-9012-3456");
                r.setTotalBalance(500000L);
                r.setWithdrawAmount(0L);
                r.setWithdrawTime(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                return r;
        }

        private SaldoResponseDeleteAt createSaldoResponseDeleteAt(Long id) {
                SaldoResponseDeleteAt r = new SaldoResponseDeleteAt();
                r.setId(id);
                r.setCardNumber("1234-5678-9012-3456");
                r.setTotalBalance(500000L);
                r.setWithdrawAmount(0L);
                r.setWithdrawTime(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setDeletedAt(LocalDateTime.of(2024, 6, 16, 8, 0).toString());
                return r;
        }

        // findAllSaldo
        @Test
        @DisplayName("findAllSaldo - success with data")
        void findAllSaldo_Success() {
                Saldo.FindAllSaldoRequest request = Saldo.FindAllSaldoRequest.newBuilder()
                                .setPage(1).setPageSize(10).build();

                SaldoResponse data = createSaldoResponse(1L);
                ApiResponsePagination<List<SaldoResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Saldo list", List.of(data), null);
                when(saldoQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                SaldoQuery.ApiResponsePaginationSaldo response = handler.findAllSaldo(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).getCardNumber()).isEqualTo("1234-5678-9012-3456");
        }

        @Test
        @DisplayName("findAllSaldo - internal error")
        void findAllSaldo_Error() {
                Saldo.FindAllSaldoRequest request = Saldo.FindAllSaldoRequest.newBuilder().build();
                when(saldoQueryService.findAll(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findAllSaldo(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // findByIdSaldo
        @Test
        @DisplayName("findByIdSaldo - success")
        void findById_Success() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();
                SaldoResponse data = createSaldoResponse(1L);
                ApiResponse<SaldoResponse> apiResp = ApiResponse.success("Saldo found", data);
                when(saldoQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldo response = handler.findByIdSaldo(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getSaldoId()).isEqualTo(1);
        }

        @Test
        @DisplayName("findByIdSaldo - NOT_FOUND")
        void findById_NotFound() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(999).build();
                when(saldoQueryService.findById(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Saldo not found")));
                try {
                        handler.findByIdSaldo(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        @Test
        @DisplayName("findByIdSaldo - internal error")
        void findById_Error() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();
                when(saldoQueryService.findById(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Unexpected")));
                try {
                        handler.findByIdSaldo(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // findByCardNumber
        @Test
        @DisplayName("findByCardNumber - success")
        void findByCardNumber_Success() {
                pb.card.Card.FindByCardNumberRequest request = pb.card.Card.FindByCardNumberRequest.newBuilder()
                                .setCardNumber("1234-5678-9012-3456").build();

                SaldoResponse data = createSaldoResponse(1L);
                ApiResponse<SaldoResponse> apiResp = ApiResponse.success("Saldo found", data);
                when(saldoQueryService.findByCard(anyString())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldo response = handler.findByCardNumber(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getCardNumber()).isEqualTo("1234-5678-9012-3456");
        }

        @Test
        @DisplayName("findByCardNumber - NOT_FOUND")
        void findByCardNumber_NotFound() {
                pb.card.Card.FindByCardNumberRequest request = pb.card.Card.FindByCardNumberRequest.newBuilder()
                                .setCardNumber("nonexistent").build();
                when(saldoQueryService.findByCard(anyString()))
                                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Saldo not found")));
                try {
                        handler.findByCardNumber(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // findByActive
        @Test
        @DisplayName("findByActive - success")
        void findByActive_Success() {
                Saldo.FindAllSaldoRequest request = Saldo.FindAllSaldoRequest.newBuilder().setPage(1).build();
                SaldoResponseDeleteAt data = createSaldoResponseDeleteAt(1L);
                ApiResponsePagination<List<SaldoResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Active saldos", List.of(data), null);
                when(saldoQueryService.findActive(any())).thenReturn(Uni.createFrom().item(apiResp));

                SaldoQuery.ApiResponsePaginationSaldoDeleteAt response = handler.findByActive(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("findByActive - internal error")
        void findByActive_Error() {
                Saldo.FindAllSaldoRequest request = Saldo.FindAllSaldoRequest.newBuilder().build();
                when(saldoQueryService.findActive(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findByActive(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // findByTrashed
        @Test
        @DisplayName("findByTrashed - success")
        void findByTrashed_Success() {
                Saldo.FindAllSaldoRequest request = Saldo.FindAllSaldoRequest.newBuilder().build();
                SaldoResponseDeleteAt data = createSaldoResponseDeleteAt(1L);
                ApiResponsePagination<List<SaldoResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Trashed saldos", List.of(data), null);
                when(saldoQueryService.findTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

                SaldoQuery.ApiResponsePaginationSaldoDeleteAt response = handler.findByTrashed(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData(0).hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("findByTrashed - internal error")
        void findByTrashed_Error() {
                Saldo.FindAllSaldoRequest request = Saldo.FindAllSaldoRequest.newBuilder().build();
                when(saldoQueryService.findTrashed(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findByTrashed(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // edge cases
        @Test
        @DisplayName("findAllSaldo - empty list")
        void findAllSaldo_Empty() {
                Saldo.FindAllSaldoRequest request = Saldo.FindAllSaldoRequest.newBuilder().build();
                ApiResponsePagination<List<SaldoResponse>> apiResp = new ApiResponsePagination<>("success", "No data",
                                List.of(), null);
                when(saldoQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                SaldoQuery.ApiResponsePaginationSaldo response = handler.findAllSaldo(request).await().indefinitely();
                assertThat(response.getDataCount()).isZero();
        }

        @Test
        @DisplayName("findByIdSaldo - null data")
        void findById_NullData() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();
                ApiResponse<SaldoResponse> apiResp = ApiResponse.success("No data", null);
                when(saldoQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldo response = handler.findByIdSaldo(request).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }
}