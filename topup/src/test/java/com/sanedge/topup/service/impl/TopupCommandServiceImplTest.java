package com.sanedge.topup.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
import com.sanedge.common.enums.Status;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.topup.domain.requests.CreateTopupRequest;
import com.sanedge.topup.domain.requests.UpdateTopupRequest;
import com.sanedge.topup.domain.response.TopupResponse;
import com.sanedge.topup.domain.response.TopupResponseDeleteAt;
import com.sanedge.topup.entity.Topup;
import com.sanedge.topup.entity.Outbox;
import com.sanedge.topup.repository.OutboxRepository;
import com.sanedge.topup.repository.TopupCommandRepository;
import com.sanedge.topup.repository.TopupQueryRepository;
import com.sanedge.topup.service.KafkaService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import pb.card.CardQueryService;
import pb.saldo.SaldoCommandService;
import pb.saldo.SaldoQueryService;

@ExtendWith(MockitoExtension.class)
class TopupCommandServiceImplTest {

        @Mock
        private CardQueryService cardQueryService;
        @Mock
        private SaldoQueryService saldoQueryService;
        @Mock
        private SaldoCommandService saldoCommandService;
        @Mock
        private TopupQueryRepository topupQueryRepo;
        @Mock
        OutboxRepository outboxRepository;
        @Mock
        private TopupCommandRepository topupCommandRepo;
        @Mock
        private Validator validator;
        @Mock
        private RedisService redisService;
        @Mock
        private KafkaService kafkaService;

        private TracingMetrics tracingMetrics;
        private TopupCommandServiceImpl service;

        @BeforeEach
        void setUp() {
                tracingMetrics = org.mockito.Mockito.mock(TracingMetrics.class,
                        org.mockito.Mockito.withSettings().lenient());

                // Stub both traceAndMeasure overloads:
                // 4-param: (String, String, Attributes, Supplier)
                lenient().doAnswer(inv -> {
                        Supplier<Uni<?>> s = inv.getArgument(3);
                        return s.get();
                }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
                // 3-param: (String, String, Supplier)
                lenient().doAnswer(inv -> {
                        Supplier<Uni<?>> s = inv.getArgument(2);
                        return s.get();
                }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());

                service = new TopupCommandServiceImpl(cardQueryService, saldoQueryService, saldoCommandService,
                                topupQueryRepo, topupCommandRepo, validator, redisService, kafkaService,
                                tracingMetrics, outboxRepository);
                lenient().when(redisService.deleteReactive(anyString()))
                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(outboxRepository.persist(any(Outbox.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Outbox) inv.getArgument(0)));
                lenient().when(topupCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
                lenient().when(topupCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));
        }

        private Topup createTopup(Long id, int amount) {
                Topup t = new Topup();
                t.setTopupId(id);
                t.setTopupNo(UUID.randomUUID());
                t.setCardNumber("1234-5678-9012-3456");
                t.setTopupAmount(amount);
                t.setTopupMethod("BANK_TRANSFER");
                t.setStatus(Status.PENDING);
                t.setTopupTime(Timestamp.valueOf(LocalDateTime.now()));
                t.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                t.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return t;
        }

        private CreateTopupRequest createValidCreateRequest() {
                CreateTopupRequest r = new CreateTopupRequest();
                r.setCardNumber("1234-5678-9012-3456");
                r.setTopupAmount(50000L);
                r.setTopupMethod("BANK_TRANSFER");
                return r;
        }

        private UpdateTopupRequest createValidUpdateRequest() {
                UpdateTopupRequest r = new UpdateTopupRequest();
                r.setTopupId(1L);
                r.setCardNumber("1234-5678-9012-3456");
                r.setTopupAmount(75000L);
                r.setTopupMethod("BANK_TRANSFER");
                return r;
        }

        private void mockCardAndSaldoForCreate() {
                pb.card.Card.CardWithEmailResponse cardResp = pb.card.Card.CardWithEmailResponse.newBuilder()
                                .setCardNumber("1234-5678-9012-3456").setEmail("test@test.com").build();
                lenient().when(cardQueryService.findUserCardByCardNumber(any()))
                                .thenReturn(Uni.createFrom().item(cardResp));
                pb.saldo.Saldo.SaldoResponse saldo = pb.saldo.Saldo.SaldoResponse.newBuilder()
                                .setCardNumber("1234-5678-9012-3456").setTotalBalance(100000).build();
                pb.saldo.Saldo.ApiResponseSaldo saldoResp = pb.saldo.Saldo.ApiResponseSaldo.newBuilder().setData(saldo)
                                .build();
                lenient().when(saldoQueryService.findByCardNumber(any())).thenReturn(Uni.createFrom().item(saldoResp));
                lenient().when(saldoCommandService.updateSaldoBalance(any()))
                                .thenReturn(Uni.createFrom().item(saldoResp));
                lenient().when(kafkaService.sendMessage(anyString(), anyString(), any()))
                                .thenReturn(Uni.createFrom().voidItem());
        }

        @Nested
        @DisplayName("create tests")
        class CreateTests {
                @Test
                void success() {
                        CreateTopupRequest req = createValidCreateRequest();
                        mockCardAndSaldoForCreate();
                        when(topupCommandRepo.persist(any(Topup.class))).thenAnswer(inv -> {
                                Topup t = inv.getArgument(0);
                                t.setTopupId(1L);
                                return Uni.createFrom().item(t);
                        });
                        ApiResponse<TopupResponse> resp = service.create(req).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                        assertThat(resp.data().getId()).isEqualTo(1L);
                        verify(redisService, atLeastOnce()).deleteReactive(anyString());
                }

                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Test
                void validationFails() {
                        CreateTopupRequest req = new CreateTopupRequest();
                        ConstraintViolation<?> v = org.mockito.Mockito.mock(ConstraintViolation.class);
                        when(v.getPropertyPath()).thenReturn(org.mockito.Mockito.mock(Path.class));
                        when(v.getMessage()).thenReturn("must not be null");
                        Set violations = new HashSet();
                        violations.add(v);
                        when(validator.validate(any())).thenReturn(violations);
                        ApiResponse<TopupResponse> resp = service.create(req).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("error");
                        assertThat(resp.message()).contains("Validation failed");
                }
        }

        @Nested
        @DisplayName("update tests")
        class UpdateTests {
                @Test
                void success() {
                        UpdateTopupRequest req = createValidUpdateRequest();
                        Topup existing = createTopup(1L, 2000000);
                        when(topupQueryRepo.findTopupById(1L)).thenReturn(Uni.createFrom().item(existing));

                        // Card response
                        pb.card.Card.ApiResponseCard cardResp = pb.card.Card.ApiResponseCard.newBuilder()
                                        .setData(pb.card.Card.CardResponse.newBuilder()
                                                        .setCardNumber("1234-5678-9012-3456").build())
                                        .build();
                        when(cardQueryService.findByCardNumber(any())).thenReturn(Uni.createFrom().item(cardResp));

                        // Saldo response with explicit saldoId
                        pb.saldo.Saldo.SaldoResponse saldo = pb.saldo.Saldo.SaldoResponse.newBuilder()
                                        .setCardNumber("1234-5678-9012-3456")
                                        .setTotalBalance(100000)
                                        .setSaldoId(99) // important
                                        .build();
                        pb.saldo.Saldo.ApiResponseSaldo saldoResp = pb.saldo.Saldo.ApiResponseSaldo.newBuilder()
                                        .setData(saldo)
                                        .build();
                        when(saldoQueryService.findByCardNumber(any())).thenReturn(Uni.createFrom().item(saldoResp));

                        when(saldoCommandService.updateSaldoBalance(any()))
                                        .thenReturn(Uni.createFrom().item(saldoResp));

                        // Use a fixed Topup response to avoid generic type inference issues
                        Topup persisted = new Topup();
                        persisted.setTopupId(1L);
                        persisted.setTopupNo(UUID.randomUUID());
                        persisted.setCardNumber("1234-5678-9012-3456");
                        persisted.setTopupAmount(req.getTopupAmount().intValue());
                        persisted.setTopupMethod("BANK_TRANSFER");
                        persisted.setStatus(Status.SUCCESS);
                        when(topupCommandRepo.persist(any(Topup.class)))
                                        .thenReturn(Uni.createFrom().item(persisted));

                        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<TopupResponse> resp = service.update(req).await().indefinitely();

                        if (!"success".equals(resp.status())) {
                                System.err.println("UPDATE ERROR: " + resp.message());
                        }

                        assertThat(resp.status()).isEqualTo("success");
                        assertThat(resp.data()).isNotNull();
                        assertThat(resp.data().getId()).isEqualTo(1L);
                        assertThat(resp.data().getTopupAmount()).isEqualTo(75000L);
                }

                @Test
                void notFound() {
                        UpdateTopupRequest req = createValidUpdateRequest();

                        pb.card.Card.ApiResponseCard cardResp = pb.card.Card.ApiResponseCard.newBuilder()
                                        .setData(pb.card.Card.CardResponse.newBuilder()
                                                        .setCardNumber("1234-5678-9012-3456").build())
                                        .build();
                        when(cardQueryService.findByCardNumber(any())).thenReturn(Uni.createFrom().item(cardResp));

                        when(topupQueryRepo.findTopupById(1L)).thenReturn(Uni.createFrom().nullItem());
                        when(topupCommandRepo.updateTopupStatus(anyLong(), anyString()))
                                        .thenReturn(Uni.createFrom().nullItem());

                        ApiResponse<TopupResponse> resp = service.update(req).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("error");
                        assertThat(resp.message()).contains("Topup not found");
                }
        }

        @Nested
        @DisplayName("trash tests")
        class TrashTests {
                @Test
                void success() {
                        Topup t = createTopup(1L, 50000);
                        t.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
                        when(topupCommandRepo.trashed(1L)).thenReturn(Uni.createFrom().item(t));
                        ApiResponse<TopupResponseDeleteAt> resp = service.trashed(1L).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                }

                @Test
                void notFound() {
                        when(topupCommandRepo.trashed(1L)).thenReturn(Uni.createFrom().nullItem());
                        ApiResponse<TopupResponseDeleteAt> resp = service.trashed(1L).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("error");
                        assertThat(resp.message()).contains("Topup not found");
                }
        }

        @Nested
        @DisplayName("restore tests")
        class RestoreTests {
                @Test
                void success() {
                        when(topupCommandRepo.restore(1L)).thenReturn(Uni.createFrom().item(createTopup(1L, 50000)));
                        ApiResponse<TopupResponseDeleteAt> resp = service.restore(1L).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                }

                @Test
                void notFoundOrNotTrashed() {
                        when(topupCommandRepo.restore(1L)).thenReturn(Uni.createFrom().nullItem());
                        assertThatThrownBy(() -> service.restore(1L).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining("must be trashed first");
                }
        }

        @Nested
        @DisplayName("deletePermanent tests")
        class DeletePermanentTests {
                @Test
                void success() {
                        Topup t = createTopup(1L, 50000);
                        t.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
                        when(topupQueryRepo.findTopupById(1L)).thenReturn(Uni.createFrom().item(t));
                        when(topupCommandRepo.deletePermanent(1L)).thenReturn(Uni.createFrom().item(true));
                        ApiResponse<Boolean> resp = service.deletePermanent(1L).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                        assertThat(resp.data()).isTrue();
                }

                @Test
                void notTrashed() {
                        when(topupQueryRepo.findTopupById(1L))
                                        .thenReturn(Uni.createFrom().item(createTopup(1L, 50000)));
                        assertThatThrownBy(() -> service.deletePermanent(1L).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class);
                }
        }

        @Nested
        @DisplayName("restoreAll tests")
        class RestoreAllTests {
                @Test
                void success() {
                        ApiResponse<Boolean> resp = service.restoreAll().await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                }

                @Test
                void noTrashed() {
                        when(topupCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
                        assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }

        @Nested
        @DisplayName("deleteAll tests")
        class DeleteAllTests {
                @Test
                void success() {
                        ApiResponse<Boolean> resp = service.deleteAll().await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                }

                @Test
                void noTrashed() {
                        when(topupCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
                        assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }
}
