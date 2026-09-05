package com.sanedge.transfer.service.impl;

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
import com.sanedge.transfer.domain.requests.CreateTransferRequest;
import com.sanedge.transfer.domain.requests.UpdateTransferRequest;
import com.sanedge.transfer.domain.response.TransferResponse;
import com.sanedge.transfer.domain.response.TransferResponseDeleteAt;
import com.sanedge.transfer.entity.Transfer;
import com.sanedge.transfer.entity.Outbox;
import com.sanedge.transfer.repository.OutboxRepository;
import com.sanedge.transfer.repository.TransferCommandRepository;
import com.sanedge.transfer.repository.TransferQueryRepository;
import com.sanedge.transfer.service.KafkaService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Validator;
import pb.card.CardQueryService;
import pb.saldo.SaldoCommandService;
import pb.saldo.SaldoQueryService;

@ExtendWith(MockitoExtension.class)
class TransferCommandServiceImplTest {

        @Mock
        private TransferQueryRepository transferQueryRepo;

        @Mock
        OutboxRepository outboxRepository;

        @Mock
        private TransferCommandRepository transferCommandRepo;

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

        private TransferCommandServiceImpl service;

        @BeforeEach
        void setUp() {
                service = new TransferCommandServiceImpl(
                                transferCommandRepo,
                                cardQueryService,
                                saldoQueryService,
                                saldoCommandService,
                                transferQueryRepo,
                                validator,
                                redisService,
                                kafkaService,
                                tracingMetrics,
                                outboxRepository);

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(3);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(2);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any());

                lenient().when(redisService.deleteReactive(anyString()))
                                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(outboxRepository.persist(any(Outbox.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Outbox) inv.getArgument(0)));

                lenient().when(transferCommandRepo.deleteAllDeleted())
                                .thenReturn(Uni.createFrom().item(true));
                lenient().when(transferCommandRepo.restoreAllDeleted())
                                .thenReturn(Uni.createFrom().item(true));
        }

        private Transfer createTestTransfer(Long id, String from, String to, int amount, Status status) {
                Transfer t = new Transfer();
                t.transferId = id;
                t.setTransferNo(UUID.randomUUID());
                t.setTransferFrom(from);
                t.setTransferTo(to);
                t.setTransferAmount(amount);
                t.setStatus(status);
                t.setTransferTime(Timestamp.valueOf(LocalDateTime.now()));
                t.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                t.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return t;
        }

        private CreateTransferRequest createValidCreateRequest() {
                CreateTransferRequest req = new CreateTransferRequest();
                req.setTransferFrom("111122223333");
                req.setTransferTo("444455556666");
                req.setTransferAmount(150000L);
                return req;
        }

        private UpdateTransferRequest createValidUpdateRequest() {
                UpdateTransferRequest req = new UpdateTransferRequest();
                req.setTransferId(1L);
                req.setTransferFrom("111122223333");
                req.setTransferTo("444455556666");
                req.setTransferAmount(200000L);
                return req;
        }

        private void mockCardAndSaldoServices() {
                pb.card.Card.CardWithEmailResponse senderCardResp = pb.card.Card.CardWithEmailResponse
                                .newBuilder()
                                .setCardNumber("111122223333")
                                .setEmail("test@test.com")
                                .build();
                lenient().when(cardQueryService.findUserCardByCardNumber(any()))
                                .thenReturn(Uni.createFrom().item(senderCardResp));

                pb.card.Card.CardResponse receiverCardData = pb.card.Card.CardResponse.newBuilder()
                                .setCardNumber("444455556666")
                                .build();
                pb.card.Card.ApiResponseCard receiverCardResp = pb.card.Card.ApiResponseCard.newBuilder()
                                .setData(receiverCardData)
                                .build();
                lenient().when(cardQueryService.findByCardNumber(any()))
                                .thenReturn(Uni.createFrom().item(receiverCardResp));

                pb.saldo.Saldo.SaldoResponse senderSaldoResp = pb.saldo.Saldo.SaldoResponse.newBuilder()
                                .setCardNumber("111122223333")
                                .setTotalBalance(500000)
                                .build();
                pb.saldo.Saldo.ApiResponseSaldo apiSaldoResp = pb.saldo.Saldo.ApiResponseSaldo.newBuilder()
                                .setData(senderSaldoResp)
                                .build();
                lenient().when(saldoQueryService.findByCardNumber(any()))
                                .thenReturn(Uni.createFrom().item(apiSaldoResp));

                lenient().when(saldoCommandService.updateSaldoBalance(any()))
                                .thenReturn(Uni.createFrom().item(apiSaldoResp));

                lenient().when(kafkaService.sendMessage(anyString(), anyString(), any()))
                                .thenReturn(Uni.createFrom().voidItem());
        }

        @Nested
        @DisplayName("create transfer tests")
        class CreateTransferTests {

                @Test
                @DisplayName("should successfully create transfer on happy path")
                void create_Success() {
                        CreateTransferRequest req = createValidCreateRequest();
                        mockCardAndSaldoServices();

                        when(transferCommandRepo.persist(any(Transfer.class)))
                                        .thenAnswer(inv -> {
                                                Transfer t = inv.getArgument(0);
                                                if (t.transferId == null)
                                                        t.transferId = 1L;
                                                return Uni.createFrom().item(t);
                                        });
                        when(transferCommandRepo.updateTransferStatus(anyLong(), anyString()))
                                        .thenAnswer(inv -> {
                                                Long id = inv.getArgument(0);
                                                Transfer t = createTestTransfer(id, "111122223333", "444455556666",
                                                                150000, Status.SUCCESS);
                                                return Uni.createFrom().item(t);
                                        });

                        ApiResponse<TransferResponse> response = service.create(req).await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transfer created successfully");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);

                        verify(redisService, atLeastOnce()).deleteReactive(anyString());
                }

                @SuppressWarnings({"unchecked", "rawtypes"})
                @Test
                @DisplayName("should fail when validation fails")
                void create_ValidationFails() {
                        CreateTransferRequest req = new CreateTransferRequest();
                        jakarta.validation.ConstraintViolation<?> violation = org.mockito.Mockito.mock(
                                        jakarta.validation.ConstraintViolation.class);
                        when(violation.getPropertyPath()).thenReturn(
                                        org.mockito.Mockito.mock(jakarta.validation.Path.class));
                        when(violation.getMessage()).thenReturn("must not be null");
                        java.util.Set violations = new java.util.HashSet();
                        violations.add(violation);
                        when(validator.validate(any())).thenReturn(violations);

                        ApiResponse<TransferResponse> response = service.create(req).await().indefinitely();

                        assertThat(response.status()).isEqualTo("error");
                        assertThat(response.message()).contains("Validation failed");
                }
        }

        @Nested
        @DisplayName("update transfer tests")
        class UpdateTransferTests {

                @Test
                @DisplayName("should fail when transfer not found")
                void update_NotFound() {
                        UpdateTransferRequest req = createValidUpdateRequest();

                        when(transferQueryRepo.findTransferById(1L))
                                        .thenReturn(Uni.createFrom().nullItem());

                        ApiResponse<TransferResponse> response = service.update(req).await().indefinitely();

                        assertThat(response.status()).isEqualTo("error");
                        assertThat(response.message()).contains("Failed to update transfer");
                }

                @Test
                @DisplayName("should fail when transfer is SUCCESS and cannot be modified")
                void update_AlreadySuccess() {
                        UpdateTransferRequest req = createValidUpdateRequest();

                        Transfer existingTransfer = createTestTransfer(1L, "111122223333", "444455556666", 150000,
                                        Status.SUCCESS);
                        when(transferQueryRepo.findTransferById(1L))
                                        .thenReturn(Uni.createFrom().item(existingTransfer));

                        ApiResponse<TransferResponse> response = service.update(req).await().indefinitely();

                        assertThat(response.status()).isEqualTo("error");
                        assertThat(response.message()).contains("Failed to update transfer");
                }

                @Test
                @DisplayName("should successfully update transfer on happy path")
                void update_Success() {
                        UpdateTransferRequest req = createValidUpdateRequest();

                        Transfer existingTransfer = createTestTransfer(1L, "111122223333", "444455556666", 150000,
                                        Status.PENDING);
                        when(transferQueryRepo.findTransferById(1L))
                                        .thenReturn(Uni.createFrom().item(existingTransfer));

                        // Mock saldo query service for update flow
                        pb.saldo.Saldo.SaldoResponse saldoResp = pb.saldo.Saldo.SaldoResponse.newBuilder()
                                        .setCardNumber("111122223333")
                                        .setTotalBalance(500000)
                                        .build();
                        pb.saldo.Saldo.ApiResponseSaldo apiSaldoResp = pb.saldo.Saldo.ApiResponseSaldo.newBuilder()
                                        .setData(saldoResp)
                                        .build();
                        when(saldoQueryService.findByCardNumber(any()))
                                        .thenReturn(Uni.createFrom().item(apiSaldoResp));

                        // Mock saldo command service
                        when(saldoCommandService.updateSaldoBalance(any()))
                                        .thenReturn(Uni.createFrom().item(apiSaldoResp));

                        when(transferCommandRepo.updateTransferStatus(anyLong(), anyString()))
                                        .thenAnswer(inv -> {
                                                Transfer t = createTestTransfer(1L, "111122223333", "444455556666",
                                                                200000, Status.SUCCESS);
                                                return Uni.createFrom().item(t);
                                        });

                        when(transferCommandRepo.persist(any(Transfer.class)))
                                        .thenAnswer(inv -> {
                                                Transfer t = inv.getArgument(0);
                                                return Uni.createFrom().item(t);
                                        });

                        ApiResponse<TransferResponse> response = service.update(req).await().indefinitely();

                        assertThat(response).isNotNull();
                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transfer updated successfully");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);
                        assertThat(response.data().getTransferAmount()).isEqualTo(200000L);

                        verify(redisService, atLeastOnce()).deleteReactive(anyString());
                }
        }

        @Nested
        @DisplayName("trash transfer tests")
        class TrashTransferTests {

                @Test
                @DisplayName("should successfully trash existing transfer")
                void trash_Success() {
                        Long id = 1L;
                        Transfer trashedTransfer = createTestTransfer(1L, "111122223333", "444455556666", 150000,
                                        Status.PENDING);
                        trashedTransfer.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

                        when(transferCommandRepo.trashed(1L)).thenReturn(Uni.createFrom().item(trashedTransfer));

                        ApiResponse<TransferResponseDeleteAt> response = service.trashed(id).await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transfer trashed successfully!");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);
                        assertThat(response.data().getDeletedAt()).isNotNull();
                }

                @Test
                @DisplayName("should fail when transfer not found for trash")
                void trash_NotFound() {
                        Long id = 999L;
                        when(transferCommandRepo.trashed(999L)).thenReturn(Uni.createFrom().nullItem());

                        ApiResponse<TransferResponseDeleteAt> response = service.trashed(id).await().indefinitely();

                        assertThat(response.status()).isEqualTo("error");
                        assertThat(response.message()).contains("Failed to trash transfer");
                }
        }

        @Nested
        @DisplayName("restore transfer tests")
        class RestoreTransferTests {

                @Test
                @DisplayName("should successfully restore trashed transfer")
                void restore_Success() {
                        Long id = 1L;
                        Transfer restoredTransfer = createTestTransfer(1L, "111122223333", "444455556666", 150000,
                                        Status.PENDING);

                        when(transferCommandRepo.restore(1L)).thenReturn(Uni.createFrom().item(restoredTransfer));

                        ApiResponse<TransferResponseDeleteAt> response = service.restore(id).await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transfer restored successfully!");
                        assertThat(response.data()).isNotNull();
                        assertThat(response.data().getId()).isEqualTo(1L);
                        assertThat(response.data().getDeletedAt()).isNull();
                }

                @Test
                @DisplayName("should fail when transfer is not trashed")
                void restore_NotTrashed() {
                        Long id = 1L;
                        when(transferCommandRepo.restore(1L)).thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.restore(id).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining("must be trashed first");
                }
        }

        @Nested
        @DisplayName("delete by id tests")
        class DeleteByIdTests {

                @Test
                @DisplayName("should successfully permanently delete trashed transfer")
                void deletePermanent_Success() {
                        Long id = 1L;
                        Transfer trashedTransfer = createTestTransfer(1L, "111122223333", "444455556666", 150000,
                                        Status.PENDING);
                        trashedTransfer.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

                        when(transferQueryRepo.findTransferById(1L)).thenReturn(Uni.createFrom().item(trashedTransfer));
                        when(transferCommandRepo.deletePermanent(1L)).thenReturn(Uni.createFrom().item(true));

                        ApiResponse<Boolean> response = service.deletePermanent(id).await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("Transfer permanently deleted!");
                        assertThat(response.data()).isTrue();
                }

                @Test
                @DisplayName("should fail when transfer not found for permanent delete")
                void deletePermanent_NotFound() {
                        Long id = 999L;
                        when(transferQueryRepo.findTransferById(999L)).thenReturn(Uni.createFrom().nullItem());

                        assertThatThrownBy(() -> service.deletePermanent(id).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining(
                                                        "Transfer not found or must be trashed before permanent deletion");
                }

                @Test
                @DisplayName("should fail when trying to permanently delete active transfer")
                void deletePermanent_NotTrashed() {
                        Long id = 1L;
                        Transfer activeTransfer = createTestTransfer(1L, "111122223333", "444455556666", 150000,
                                        Status.PENDING);
                        when(transferQueryRepo.findTransferById(1L)).thenReturn(Uni.createFrom().item(activeTransfer));

                        assertThatThrownBy(() -> service.deletePermanent(id).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining(
                                                        "Transfer not found or must be trashed before permanent deletion");
                }
        }

        @Nested
        @DisplayName("delete all trashed tests")
        class DeleteAllTests {

                @Test
                @DisplayName("should successfully delete all trashed transfers")
                void deleteAll_Success() {
                        ApiResponse<Boolean> response = service.deleteAll().await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("All transfers permanently deleted!");
                        assertThat(response.data()).isTrue();
                }

                @Test
                @DisplayName("should fail when no trashed transfers to delete")
                void deleteAll_NoTrashed() {
                        when(transferCommandRepo.deleteAllDeleted())
                                        .thenReturn(Uni.createFrom().item(false));

                        assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("No trashed transfers found");
                }
        }

        @Nested
        @DisplayName("restore all trashed tests")
        class RestoreAllTests {

                @Test
                @DisplayName("should successfully restore all trashed transfers")
                void restoreAll_Success() {
                        ApiResponse<Boolean> response = service.restoreAll().await().indefinitely();

                        assertThat(response.status()).isEqualTo("success");
                        assertThat(response.message()).isEqualTo("All transfers restored successfully!");
                        assertThat(response.data()).isTrue();
                }

                @Test
                @DisplayName("should fail when no trashed transfers to restore")
                void restoreAll_NoTrashed() {
                        when(transferCommandRepo.restoreAllDeleted())
                                        .thenReturn(Uni.createFrom().item(false));

                        assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("No trashed transfers found");
                }
        }
}