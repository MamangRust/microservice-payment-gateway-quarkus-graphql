package com.sanedge.withdraw.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
import com.sanedge.withdraw.domain.requests.CreateWithdrawRequest;
import com.sanedge.withdraw.domain.requests.UpdateWithdrawRequest;
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;
import com.sanedge.withdraw.entity.Withdraw;
import com.sanedge.withdraw.entity.Outbox;
import com.sanedge.withdraw.repository.OutboxRepository;
import com.sanedge.withdraw.repository.WithdrawCommandRepository;
import com.sanedge.withdraw.repository.WithdrawQueryRepository;
import com.sanedge.withdraw.service.KafkaService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Validator;
import pb.card.CardQueryService;
import pb.saldo.SaldoCommandService;
import pb.saldo.SaldoQueryService;

@ExtendWith(MockitoExtension.class)
class WithdrawCommandServiceImplTest {

        @Mock
        private WithdrawQueryRepository withdrawQueryRepository;

        @Mock
        OutboxRepository outboxRepository;

        @Mock
        private WithdrawCommandRepository withdrawCommandRepo;

        @Mock
        private CardQueryService cardQueryService;

        @Mock
        private SaldoQueryService saldoQueryService;

        @Mock
        private SaldoCommandService saldoCommandService;

        @Mock
        private Validator validator;

        @Mock
        private RedisService redisService;

        @Mock
        private KafkaService kafkaService;

        @Mock
        private TracingMetrics tracingMetrics;

        private WithdrawCommandServiceImpl service;

        @BeforeEach
        void setUp() {
                service = new WithdrawCommandServiceImpl(
                                withdrawQueryRepository,
                                withdrawCommandRepo,
                                cardQueryService,
                                saldoQueryService,
                                saldoCommandService,
                                validator,
                                redisService,
                                kafkaService,
                                tracingMetrics,
                                outboxRepository);

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(3);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(
                                                anyString(),
                                                anyString(),
                                                any(Attributes.class),
                                                any());

                lenient().when(redisService.deleteReactive(anyString()))
                                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(outboxRepository.persist(any(Outbox.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Outbox) inv.getArgument(0)));

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(2);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any());

                lenient().when(withdrawCommandRepo.deleteAllDeleted())
                                .thenReturn(Uni.createFrom().item(true));
                lenient().when(withdrawCommandRepo.restoreAllDeleted())
                                .thenReturn(Uni.createFrom().item(true));
        }

        private Withdraw createTestWithdraw(Long id, String cardNumber, Long amount, Status status) {
                Withdraw w = new Withdraw();
                w.withdrawId = id;
                w.setWithdrawNo(UUID.randomUUID()); // FIX: NPE getWithdrawNo()
                w.setCardNumber(cardNumber);
                w.setWithdrawAmount(amount.intValue());
                w.setStatus(status);
                w.setWithdrawTime(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30, 0)));
                w.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                w.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return w;
        }

        private CreateWithdrawRequest createValidCreateRequest() {
                CreateWithdrawRequest req = new CreateWithdrawRequest();
                req.setCardNumber("1234567890123456");
                req.setWithdrawAmount(150000L);
                req.setWithdrawTime(LocalDateTime.of(2024, 6, 15, 10, 30, 0));
                return req;
        }

        private UpdateWithdrawRequest createValidUpdateRequest() {
                UpdateWithdrawRequest req = new UpdateWithdrawRequest();
                req.setWithdrawId(1L);
                req.setCardNumber("1234567890123456");
                req.setWithdrawAmount(200000L);
                req.setWithdrawTime(LocalDateTime.of(2024, 6, 16, 12, 0, 0));
                return req;
        }

        @Nested
        @DisplayName("create withdraw tests")
        class CreateWithdrawTests {

                @Test
                @DisplayName("should successfully create withdraw")
                void create_Success() {
                        CreateWithdrawRequest req = createValidCreateRequest();

                        // Mock card service
                        pb.card.Card.CardWithEmailResponse cardWithEmailResp = pb.card.Card.CardWithEmailResponse
                                        .newBuilder()
                                        .setCardNumber("1234567890123456")
                                        .setEmail("test@example.com")
                                        .build();
                        when(cardQueryService.findUserCardByCardNumber(any()))
                                        .thenReturn(Uni.createFrom().item(cardWithEmailResp));

                        // Mock saldo query service
                        pb.saldo.Saldo.SaldoResponse saldoResp = pb.saldo.Saldo.SaldoResponse.newBuilder()
                                        .setCardNumber("1234567890123456")
                                        .setTotalBalance(500000)
                                        .build();
                        pb.saldo.Saldo.ApiResponseSaldo apiSaldoResp = pb.saldo.Saldo.ApiResponseSaldo.newBuilder()
                                        .setData(saldoResp)
                                        .build();
                        when(saldoQueryService.findByCardNumber(any()))
                                        .thenReturn(Uni.createFrom().item(apiSaldoResp));

                        // Mock saldo command service
                        when(saldoCommandService.updateSaldoWithdraw(any()))
                                        .thenReturn(Uni.createFrom().item(apiSaldoResp));

                        // Mock withdraw repo
                        when(withdrawCommandRepo.persist(any(Withdraw.class)))
                                        .thenAnswer(inv -> {
                                                Withdraw w = inv.getArgument(0);
                                                if (w.withdrawId == null)
                                                        w.withdrawId = 1L;
                                                return Uni.createFrom().item(w);
                                        });
                        when(withdrawCommandRepo.updateStatus(any(), anyString()))
                                        .thenAnswer(inv -> {
                                                Withdraw w = new Withdraw();
                                                w.withdrawId = 1L;
                                                w.setWithdrawNo(UUID.randomUUID());
                                                w.setCardNumber("1234567890123456");
                                                w.setWithdrawAmount(150000);
                                                w.setStatus(Status.SUCCESS);
                                                w.setWithdrawTime(
                                                                Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30, 0)));
                                                return Uni.createFrom().item(w);
                                        });

                        // Mock kafka (fire-and-forget)
                        lenient().when(kafkaService.sendMessage(anyString(), anyString(), any()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<WithdrawResponse> response = service.create(req).await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Created withdraw successfully");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);
                        assertThat(response.data().getCardNumber()).isEqualTo("1234567890123456");
                        assertThat(response.data().getWithdrawAmount()).isEqualTo(150000L);

                        verify(redisService, atLeastOnce()).deleteReactive(anyString());
                }
        }

        @Nested
        @DisplayName("update withdraw tests")
        class UpdateWithdrawTests {

                @Test
                @DisplayName("should fail when withdraw not found")
                void update_NotFound() {
                        UpdateWithdrawRequest req = createValidUpdateRequest();

                        pb.card.Card.CardResponse cardResp = pb.card.Card.CardResponse.newBuilder()
                                        .setCardNumber("1234567890123456").build();
                        pb.card.Card.ApiResponseCard apiResp = pb.card.Card.ApiResponseCard.newBuilder()
                                        .setData(cardResp).build();
                        when(cardQueryService.findByCardNumber(any()))
                                        .thenReturn(Uni.createFrom().item(apiResp));
                        when(withdrawQueryRepository.findById(1L))
                                        .thenReturn(Uni.createFrom().nullItem());

                        ApiResponse<WithdrawResponse> response = service.update(req).await().indefinitely();
                        assertThat(response.status()).isEqualTo("error");
                        assertThat(response.message()).contains("Failed to update withdraw");
                }

                @Test
                @DisplayName("should fail when withdraw is SUCCESS and cannot be modified")
                void update_AlreadySuccess() {
                        UpdateWithdrawRequest req = createValidUpdateRequest();

                        Withdraw existingWithdraw = createTestWithdraw(1L, "1234567890123456", 150000L, Status.SUCCESS);

                        pb.card.Card.CardResponse cardResp = pb.card.Card.CardResponse.newBuilder()
                                        .setCardNumber("1234567890123456").build();
                        pb.card.Card.ApiResponseCard apiResp = pb.card.Card.ApiResponseCard.newBuilder()
                                        .setData(cardResp).build();
                        when(cardQueryService.findByCardNumber(any()))
                                        .thenReturn(Uni.createFrom().item(apiResp));
                        when(withdrawQueryRepository.findById(1L))
                                        .thenReturn(Uni.createFrom().item(existingWithdraw));

                        ApiResponse<WithdrawResponse> response = service.update(req).await().indefinitely();
                        assertThat(response.status()).isEqualTo("error");
                        assertThat(response.message()).contains("Failed to update withdraw");
                }

                @Test
                @DisplayName("should fail when withdraw is FAILED and cannot be modified")
                void update_AlreadyFailed() {
                        UpdateWithdrawRequest req = createValidUpdateRequest();

                        Withdraw existingWithdraw = createTestWithdraw(1L, "1234567890123456", 150000L, Status.FAILED);

                        pb.card.Card.CardResponse cardResp = pb.card.Card.CardResponse.newBuilder()
                                        .setCardNumber("1234567890123456").build();
                        pb.card.Card.ApiResponseCard apiResp = pb.card.Card.ApiResponseCard.newBuilder()
                                        .setData(cardResp).build();
                        when(cardQueryService.findByCardNumber(any()))
                                        .thenReturn(Uni.createFrom().item(apiResp));
                        when(withdrawQueryRepository.findById(1L))
                                        .thenReturn(Uni.createFrom().item(existingWithdraw));

                        ApiResponse<WithdrawResponse> response = service.update(req).await().indefinitely();
                        assertThat(response.status()).isEqualTo("error");
                        assertThat(response.message()).contains("Failed to update withdraw");
                }

                @Test
                @DisplayName("should successfully update withdraw on happy path")
                void update_Success() {
                        UpdateWithdrawRequest req = createValidUpdateRequest();

                        Withdraw existingWithdraw = createTestWithdraw(1L, "1234567890123456", 150000L, Status.PENDING);

                        // Mock card service
                        pb.card.Card.CardResponse cardResp = pb.card.Card.CardResponse.newBuilder()
                                        .setCardNumber("1234567890123456").build();
                        pb.card.Card.ApiResponseCard apiCardResp = pb.card.Card.ApiResponseCard.newBuilder()
                                        .setData(cardResp).build();
                        when(cardQueryService.findByCardNumber(any()))
                                        .thenReturn(Uni.createFrom().item(apiCardResp));

                        when(withdrawQueryRepository.findById(1L))
                                        .thenReturn(Uni.createFrom().item(existingWithdraw));

                        // Mock saldo query service
                        pb.saldo.Saldo.SaldoResponse saldoResp = pb.saldo.Saldo.SaldoResponse.newBuilder()
                                        .setCardNumber("1234567890123456")
                                        .setTotalBalance(500000)
                                        .build();
                        pb.saldo.Saldo.ApiResponseSaldo apiSaldoResp = pb.saldo.Saldo.ApiResponseSaldo.newBuilder()
                                        .setData(saldoResp)
                                        .build();
                        when(saldoQueryService.findByCardNumber(any()))
                                        .thenReturn(Uni.createFrom().item(apiSaldoResp));

                        // Mock saldo command service
                        when(saldoCommandService.updateSaldoWithdraw(any()))
                                        .thenReturn(Uni.createFrom().item(apiSaldoResp));

                        when(withdrawCommandRepo.persist(any(Withdraw.class)))
                                        .thenAnswer(inv -> {
                                                Withdraw w = inv.getArgument(0);
                                                return Uni.createFrom().item(w);
                                        });
                        when(withdrawCommandRepo.updateStatus(any(), anyString()))
                                        .thenAnswer(inv -> {
                                                Withdraw w = new Withdraw();
                                                w.withdrawId = 1L;
                                                w.setWithdrawNo(UUID.randomUUID());
                                                w.setCardNumber("1234567890123456");
                                                w.setWithdrawAmount(200000);
                                                w.setStatus(Status.SUCCESS);
                                                return Uni.createFrom().item(w);
                                        });

                        ApiResponse<WithdrawResponse> response = service.update(req).await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Updated withdraw successfully");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);
                        assertThat(response.data().getWithdrawAmount()).isEqualTo(200000L);

                        verify(redisService, atLeastOnce()).deleteReactive(anyString());
                }
        }

        @Nested
        @DisplayName("trash withdraw tests")
        class TrashWithdrawTests {

                @Test
                @DisplayName("should successfully trash existing withdraw")
                void trash_Success() {
                        Long id = 1L;
                        Withdraw trashedWithdraw = createTestWithdraw(1L, "1234567890123456", 150000L, Status.PENDING);
                        trashedWithdraw.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

                        when(withdrawCommandRepo.trashed(1L)).thenReturn(Uni.createFrom().item(trashedWithdraw));

                        ApiResponse<WithdrawResponseDeleteAt> response = service.trashed(id).await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Withdraw trashed successfully!");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);
                        assertThat(response.data().getDeletedAt()).isNotNull();
                }

                @Test
                @DisplayName("should fail when withdraw not found for trash")
                void trash_NotFound() {
                        Long id = 999L;
                        when(withdrawCommandRepo.trashed(999L)).thenReturn(Uni.createFrom().nullItem());

                        ApiResponse<WithdrawResponseDeleteAt> response = service.trashed(id).await().indefinitely();
                        assertThat(response.status()).isEqualTo("error");
                        assertThat(response.message()).contains("Failed to trash withdraw");
                }
        }

        @Nested
        @DisplayName("restore withdraw tests")
        class RestoreWithdrawTests {

                @Test
                @DisplayName("should successfully restore trashed withdraw")
                void restore_Success() {
                        Long id = 1L;
                        Withdraw restoredWithdraw = createTestWithdraw(1L, "1234567890123456", 150000L, Status.PENDING);

                        when(withdrawCommandRepo.restore(1L)).thenReturn(Uni.createFrom().item(restoredWithdraw));

                        ApiResponse<WithdrawResponseDeleteAt> response = service.restore(id).await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Withdraw restored successfully!");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);
                        assertThat(response.data().getDeletedAt()).isNull();
                }

                @Test
                @DisplayName("should fail when withdraw is not trashed")
                void restore_NotTrashed() {
                        Long id = 1L;
                        when(withdrawCommandRepo.restore(1L)).thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.restore(id).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining("must be trashed first");
                }
        }

        @Nested
        @DisplayName("delete by id tests")
        class DeleteByIdTests {

                @Test
                @DisplayName("should successfully permanently delete trashed withdraw")
                void deletePermanent_Success() {
                        Long id = 1L;

                        // FIX: Service memanggil findById terlebih dahulu untuk cek apakah benar-benar
                        // trashed
                        Withdraw trashedWithdraw = createTestWithdraw(1L, "1234567890123456", 150000L, Status.PENDING);
                        trashedWithdraw.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
                        when(withdrawQueryRepository.findById(1L)).thenReturn(Uni.createFrom().item(trashedWithdraw));

                        when(withdrawCommandRepo.deletePermanent(1L)).thenReturn(Uni.createFrom().item(true));

                        ApiResponse<Boolean> response = service.deletePermanent(id).await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Withdraw permanently deleted!");
                        assertThat(response.data()).isTrue();
                }

                @Test
                @DisplayName("should fail when withdraw not found for permanent delete")
                void deletePermanent_NotFound() {
                        Long id = 999L;

                        // FIX: Hanya perlu mock findById return null, hapus unnecessary stubbing
                        when(withdrawQueryRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.deletePermanent(id).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining(
                                                        "Withdraw not found or must be trashed before permanent deletion");
                }

                @Test
                @DisplayName("should fail when trying to permanently delete active withdraw")
                void deletePermanent_NotTrashed() {
                        Long id = 1L;

                        // FIX: Mock findById return active withdraw (deletedAt null), hapus unnecessary
                        // stubbing
                        Withdraw activeWithdraw = createTestWithdraw(1L, "1234567890123456", 150000L, Status.PENDING);
                        when(withdrawQueryRepository.findById(1L)).thenReturn(Uni.createFrom().item(activeWithdraw));

                        assertThatThrownBy(() -> service.deletePermanent(id).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining(
                                                        "Withdraw not found or must be trashed before permanent deletion");
                }
        }

        @Nested
        @DisplayName("delete all trashed tests")
        class DeleteAllTests {

                @Test
                @DisplayName("should successfully delete all trashed withdraws")
                void deleteAll_Success() {
                        ApiResponse<Boolean> response = service.deleteAll().await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("All withdraws permanently deleted!");
                        assertThat(response.data()).isTrue();
                }

                @Test
                @DisplayName("should fail when no trashed withdraws to delete")
                void deleteAll_NoTrashed() {
                        when(withdrawCommandRepo.deleteAllDeleted())
                                        .thenReturn(Uni.createFrom().item(false));

                        assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("No trashed withdraws found");
                }
        }

        @Nested
        @DisplayName("restore all trashed tests")
        class RestoreAllTests {

                @Test
                @DisplayName("should successfully restore all trashed withdraws")
                void restoreAll_Success() {
                        ApiResponse<Boolean> response = service.restoreAll().await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("All withdraws restored successfully!");
                        assertThat(response.data()).isTrue();
                }

                @Test
                @DisplayName("should fail when no trashed withdraws to restore")
                void restoreAll_NoTrashed() {
                        when(withdrawCommandRepo.restoreAllDeleted())
                                        .thenReturn(Uni.createFrom().item(false));

                        assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("No trashed withdraws found");
                }
        }
}