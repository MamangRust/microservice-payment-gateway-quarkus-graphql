package com.sanedge.saldo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.saldo.domain.requests.CreateSaldoRequest;
import com.sanedge.saldo.domain.requests.UpdateSaldoRequest;
import com.sanedge.saldo.domain.requests.UpdateSaldoBalance;
import com.sanedge.saldo.domain.requests.UpdateSaldoWithdraw;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;
import com.sanedge.saldo.entity.Saldo;
import com.sanedge.saldo.entity.Outbox;
import com.sanedge.saldo.repository.OutboxRepository;
import com.sanedge.saldo.repository.SaldoCommandRepository;
import com.sanedge.saldo.repository.SaldoQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.card.CardQueryService;

@ExtendWith(MockitoExtension.class)
class SaldoCommandServiceImplTest {

    @Mock
    private CardQueryService cardQueryService;
    @Mock
    private SaldoCommandRepository saldoCommandRepo;
    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private SaldoQueryRepository saldoQueryRepo;
    @Mock
    private RedisService redisService;

    private TracingMetrics tracingMetrics;

    private SaldoCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        tracingMetrics = org.mockito.Mockito.mock(TracingMetrics.class,
                org.mockito.Mockito.withSettings().lenient());

        // Lenient stubbing for tracingMetrics – the service uses both two-argument and
        // three-argument versions
        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Uni<?>> s = inv.getArgument(3);
            return s.get();
        })
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Uni<?>> s = inv.getArgument(2);
            return s.get();
        })
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());

        service = new SaldoCommandServiceImpl(cardQueryService, saldoCommandRepo, saldoQueryRepo, redisService,
                tracingMetrics, outboxRepository);
        lenient().when(redisService.deleteReactive(anyString()))
                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(outboxRepository.persist(any(Outbox.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Outbox) inv.getArgument(0)));
        lenient().when(saldoQueryRepo.findByCardNumber(anyString())).thenReturn(Uni.createFrom().nullItem());
        lenient().when(saldoCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(saldoCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));
    }

    private Saldo createSaldo(Long id, String card, int balance) {
        Saldo s = new Saldo();
        s.setSaldoId(id);
        s.setCardNumber(card);
        s.setTotalBalance(balance);
        s.setWithdrawAmount(0);
        s.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        s.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return s;
    }

    // helpers to create requests
    private CreateSaldoRequest createReq() {
        CreateSaldoRequest r = new CreateSaldoRequest();
        r.setCardNumber("1234-5678-9012-3456");
        r.setTotalBalance(100000L);
        return r;
    }

    private UpdateSaldoRequest updateReq() {
        UpdateSaldoRequest r = new UpdateSaldoRequest();
        r.setSaldoId(1L);
        r.setCardNumber("1234-5678-9012-3456");
        r.setTotalBalance(150000L);
        return r;
    }

    private UpdateSaldoBalance balanceReq(String card, int balance) {
        UpdateSaldoBalance r = new UpdateSaldoBalance();
        r.setCardNumber(card);
        r.setTotalBalance((long) balance);
        return r;
    }

    private UpdateSaldoWithdraw withdrawReq(String card, int balance, int amount) {
        UpdateSaldoWithdraw r = new UpdateSaldoWithdraw();
        r.setCardNumber(card);
        r.setTotalBalance((long) balance);
        r.setWithdrawAmount((long) amount);
        r.setWithdrawTime(LocalDateTime.now());
        return r;
    }

    // ----- create tests -----
    @Nested
    @DisplayName("create tests")
    class CreateTests {
        @Test
        void success() {
            CreateSaldoRequest req = createReq();
            pb.card.Card.ApiResponseCard cardResp = pb.card.Card.ApiResponseCard.newBuilder()
                    .setData(pb.card.Card.CardResponse.newBuilder().setCardNumber(req.getCardNumber()).build())
                    .build();
            when(cardQueryService.findByCardNumber(any())).thenReturn(Uni.createFrom().item(cardResp));
            when(saldoCommandRepo.persist(any(Saldo.class))).thenAnswer(inv -> {
                Saldo s = inv.getArgument(0);
                s.setSaldoId(1L);
                return Uni.createFrom().item(s);
            });

            ApiResponse<SaldoResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getId()).isEqualTo(1L);
            assertThat(resp.data().getCardNumber()).isEqualTo(req.getCardNumber());
        }

        @Test
        void cardNotFound_returnsError() {
            CreateSaldoRequest req = createReq();
            when(cardQueryService.findByCardNumber(any())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<SaldoResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Card not found");
        }
    }

    // ----- update tests -----
    @Nested
    @DisplayName("update tests")
    class UpdateTests {
        @Test
        void success() {
            UpdateSaldoRequest req = updateReq();
            pb.card.Card.ApiResponseCard cardResp = pb.card.Card.ApiResponseCard.newBuilder()
                    .setData(pb.card.Card.CardResponse.newBuilder().setCardNumber(req.getCardNumber()).build())
                    .build();
            when(cardQueryService.findByCardNumber(any())).thenReturn(Uni.createFrom().item(cardResp));
            when(saldoQueryRepo.findById(1L))
                    .thenReturn(Uni.createFrom().item(createSaldo(1L, req.getCardNumber(), 100000)));
            Saldo persisted = new Saldo();
            persisted.setSaldoId(1L);
            persisted.setCardNumber(req.getCardNumber());
            persisted.setTotalBalance(150000);
            persisted.setWithdrawAmount(0);
            when(saldoCommandRepo.persist(any(Saldo.class)))
                    .thenReturn(Uni.createFrom().item(persisted));

            ApiResponse<SaldoResponse> resp = service.update(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getTotalBalance()).isEqualTo(150000);
        }

        @Test
        void notFound() {
            UpdateSaldoRequest req = updateReq();
            pb.card.Card.ApiResponseCard cardResp = pb.card.Card.ApiResponseCard.newBuilder()
                    .setData(pb.card.Card.CardResponse.newBuilder().setCardNumber(req.getCardNumber()).build())
                    .build();
            when(cardQueryService.findByCardNumber(any())).thenReturn(Uni.createFrom().item(cardResp));
            when(saldoQueryRepo.findById(1L)).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<SaldoResponse> resp = service.update(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Saldo not found");
        }

        @Test
        void nullId_returnsError() {
            UpdateSaldoRequest req = updateReq();
            req.setSaldoId(null);
            ApiResponse<SaldoResponse> resp = service.update(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("saldo_id is required");
        }
    }

    // ----- trash tests -----
    @Nested
    @DisplayName("trash tests")
    class TrashTests {
        @Test
        void success() {
            Saldo trashed = createSaldo(1L, "card", 100);
            trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
            when(saldoCommandRepo.trashed(1L)).thenReturn(Uni.createFrom().item(trashed));

            ApiResponse<SaldoResponseDeleteAt> resp = service.trash(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getDeletedAt()).isNotNull();
        }

        @Test
        void notFound() {
            when(saldoCommandRepo.trashed(1L)).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<SaldoResponseDeleteAt> resp = service.trash(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Saldo not found");
        }
    }

    // ----- restore tests -----
    @Nested
    @DisplayName("restore tests")
    class RestoreTests {
        @Test
        void success() {
            when(saldoCommandRepo.restore(1L)).thenReturn(Uni.createFrom().item(createSaldo(1L, "card", 100)));
            ApiResponse<SaldoResponseDeleteAt> resp = service.restore(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
        }

        @Test
        void notFoundOrNotTrashed() {
            when(saldoCommandRepo.restore(1L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.restore(1L).await().indefinitely())
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("must be trashed first");
        }
    }

    // ----- delete (permanent) tests -----
    @Nested
    @DisplayName("delete tests")
    class DeleteTests {
        @Test
        void success() {
            Saldo trashed = createSaldo(1L, "card", 100);
            trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
            when(saldoQueryRepo.findById(1L)).thenReturn(Uni.createFrom().item(trashed));
            when(saldoCommandRepo.deletePermanent(1L)).thenReturn(Uni.createFrom().item(true));

            ApiResponse<Boolean> resp = service.delete(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void notTrashed_throws() {
            when(saldoQueryRepo.findById(1L)).thenReturn(Uni.createFrom().item(createSaldo(1L, "card", 100)));
            assertThatThrownBy(() -> service.delete(1L).await().indefinitely())
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("must be trashed");
        }
    }

    // ----- restoreAll tests -----
    @Nested
    @DisplayName("restoreAll tests")
    class RestoreAllTests {
        @Test
        void success() {
            ApiResponse<Boolean> resp = service.restoreAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
        }

        @Test
        void noTrashed_throws() {
            when(saldoCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ----- deleteAll tests -----
    @Nested
    @DisplayName("deleteAll tests")
    class DeleteAllTests {
        @Test
        void success() {
            ApiResponse<Boolean> resp = service.deleteAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
        }

        @Test
        void noTrashed_throws() {
            when(saldoCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ----- updateSaldoBalance tests -----
    @Nested
    @DisplayName("updateSaldoBalance tests")
    class UpdateSaldoBalanceTests {
        @Test
        void success() {
            UpdateSaldoBalance req = balanceReq("card-1", 200000);
            when(saldoQueryRepo.findByCardNumber(anyString()))
                    .thenReturn(Uni.createFrom().item(createSaldo(1L, "card-1", 50000)));
            Saldo persisted = new Saldo();
            persisted.setSaldoId(1L);
            persisted.setCardNumber("card-1");
            persisted.setTotalBalance(200000);
            persisted.setWithdrawAmount(0);
            when(saldoCommandRepo.persist(any(Saldo.class)))
                    .thenReturn(Uni.createFrom().item(persisted));

            ApiResponse<SaldoResponse> resp = service.updateSaldoBalance(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getTotalBalance()).isEqualTo(200000);
        }

        @Test
        void notFound() {
            UpdateSaldoBalance req = balanceReq("card-1", 200000);
            when(saldoQueryRepo.findByCardNumber(anyString())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<SaldoResponse> resp = service.updateSaldoBalance(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Saldo not found");
        }
    }

    @Nested
    @DisplayName("updateSaldoWithdraw tests")
    class UpdateSaldoWithdrawTests {
        @Test
        void success() {
            UpdateSaldoWithdraw req = withdrawReq("card-1", 150000, 5000);
            when(saldoQueryRepo.findByCardNumber(anyString()))
                    .thenReturn(Uni.createFrom().item(createSaldo(1L, "card-1", 100000)));
            Saldo persisted = new Saldo();
            persisted.setSaldoId(1L);
            persisted.setCardNumber("card-1");
            persisted.setTotalBalance(150000);
            persisted.setWithdrawAmount(5000);
            persisted.setWithdrawTime(Timestamp.valueOf(LocalDateTime.now()));
            when(saldoCommandRepo.persist(any(Saldo.class)))
                    .thenReturn(Uni.createFrom().item(persisted));

            ApiResponse<SaldoResponse> resp = service.updateSaldoWithdraw(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getTotalBalance()).isEqualTo(150000);
            assertThat(resp.data().getWithdrawAmount()).isEqualTo(5000);
        }

        @Test
        void notFound() {
            UpdateSaldoWithdraw req = withdrawReq("card-1", 150000, 5000);
            when(saldoQueryRepo.findByCardNumber(anyString())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<SaldoResponse> resp = service.updateSaldoWithdraw(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Saldo not found");
        }
    }
}