package com.sanedge.merchant.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
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
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant.domain.requests.CreateMerchantRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantRequest;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.entity.Outbox;
import com.sanedge.merchant.repository.OutboxRepository;
import com.sanedge.merchant.repository.MerchantCommandRepository;
import com.sanedge.merchant.repository.MerchantQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.user.UserQueryService;

@ExtendWith(MockitoExtension.class)
class MerchantCommandServiceImplTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private MerchantQueryRepository merchantQueryRepo;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private MerchantCommandRepository merchantCommandRepo;

    @Mock
    private RedisService redisService;

    private TracingMetrics tracingMetrics;

    private MerchantCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        tracingMetrics = mock(TracingMetrics.class, withSettings().lenient());
        lenient().doAnswer(inv -> {
            Supplier<Uni<?>> s = inv.getArgument(3);
            return s.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().doAnswer(inv -> {
            Supplier<Uni<?>> s = inv.getArgument(2);
            return s.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());

        service = new MerchantCommandServiceImpl(
                userQueryService,
                merchantQueryRepo,
                merchantCommandRepo,
                redisService,
                tracingMetrics,
                outboxRepository);

        lenient().when(redisService.deleteReactive(anyString()))
                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(outboxRepository.persist(any(Outbox.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Outbox) inv.getArgument(0)));
        lenient().when(merchantCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(merchantCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));
    }

    private Merchant createMerchant(Long id, String name, String apiKey) {
        Merchant m = new Merchant();
        m.setMerchantId(id);
        m.setMerchantNo(UUID.randomUUID());
        m.setName(name);
        m.setApiKey(apiKey);
        m.setUserId(100);
        m.setStatus(Status.SUCCESS);
        m.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        m.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return m;
    }

    private Merchant createPersistedMerchant(Long id, String name, String apiKey) {
        Merchant m = new Merchant();
        m.setMerchantId(id);
        m.setMerchantNo(UUID.randomUUID());
        m.setName(name);
        m.setApiKey(apiKey);
        m.setUserId(100);
        m.setStatus(Status.SUCCESS);
        m.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        m.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return m;
    }

    private CreateMerchantRequest createReq(String name, Long userId) {
        CreateMerchantRequest r = new CreateMerchantRequest();
        r.setName(name);
        r.setUserId(userId);
        return r;
    }

    private UpdateMerchantRequest updateReq(Long id, String name, Long userId, String status) {
        UpdateMerchantRequest r = new UpdateMerchantRequest();
        r.setMerchantId(id);
        r.setName(name);
        r.setUserId(userId);
        r.setStatus(status);
        return r;
    }

    @Nested
    @DisplayName("createMerchant tests")
    class CreateMerchantTests {
        @Test
        void success() {
            CreateMerchantRequest req = createReq("New Merchant", 10L);
            pb.user.User.ApiResponseUser userResp = pb.user.User.ApiResponseUser.newBuilder()
                    .setData(pb.user.User.UserResponse.newBuilder().setId(10).build())
                    .build();
            when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResp));
            when(merchantQueryRepo.existsByName(anyString())).thenReturn(Uni.createFrom().item(false));
            Merchant persisted = createPersistedMerchant(1L, "New Merchant", "dummy-key");
            when(merchantCommandRepo.persist(any(Merchant.class))).thenReturn(Uni.createFrom().item(persisted));

            ApiResponse<MerchantResponse> resp = service.createMerchant(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getName()).isEqualTo("New Merchant");
            assertThat(resp.data().getApiKey()).isNotNull();
        }

        @Test
        void userNotFound_throwsException() {
            CreateMerchantRequest req = createReq("Merchant", 99L);
            when(userQueryService.findById(any())).thenReturn(Uni.createFrom().nullItem());
            try {
                service.createMerchant(req).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected ResourceNotFoundException");
            } catch (ResourceNotFoundException e) {
                assertThat(e.getMessage()).contains("User not found");
            }
        }

        @Test
        void nameAlreadyExists_throwsException() {
            CreateMerchantRequest req = createReq("Existing", 10L);
            pb.user.User.ApiResponseUser userResp = pb.user.User.ApiResponseUser.newBuilder()
                    .setData(pb.user.User.UserResponse.newBuilder().setId(10).build())
                    .build();
            when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResp));
            when(merchantQueryRepo.existsByName("Existing")).thenReturn(Uni.createFrom().item(true));
            try {
                service.createMerchant(req).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected ResourceAlreadyExistsException");
            } catch (ResourceAlreadyExistsException e) {
                assertThat(e.getMessage()).contains("name already taken");
            }
        }
    }

    @Nested
    @DisplayName("updateMerchant tests")
    class UpdateMerchantTests {
        @Test
        void success() {
            UpdateMerchantRequest req = updateReq(1L, "Updated", 100L, "SUCCESS");
            Merchant existing = createMerchant(1L, "Old", "old-key");
            when(merchantQueryRepo.findMerchantById(1L)).thenReturn(Uni.createFrom().item(existing));

            pb.user.User.ApiResponseUser userResp = pb.user.User.ApiResponseUser.newBuilder()
                    .setData(pb.user.User.UserResponse.newBuilder().setId(100).build())
                    .build();
            when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResp));
            when(merchantCommandRepo.persist(any(Merchant.class)))
                    .thenReturn(Uni.createFrom().item(createPersistedMerchant(1L, "Updated", "old-key")));

            ApiResponse<MerchantResponse> resp = service.updateMerchant(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getName()).isEqualTo("Updated");
        }

        @Test
        void merchantNotFound_throwsException() {
            UpdateMerchantRequest req = updateReq(999L, "X", null, "SUCCESS");
            when(merchantQueryRepo.findMerchantById(999L)).thenReturn(Uni.createFrom().nullItem());
            try {
                service.updateMerchant(req).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected ResourceNotFoundException");
            } catch (ResourceNotFoundException e) {
                assertThat(e.getMessage()).contains("Merchant not found");
            }
        }

        @Test
        void userNotFoundInUpdate_throwsException() {
            UpdateMerchantRequest req = updateReq(1L, "Y", 200L, "SUCCESS");
            Merchant existing = createMerchant(1L, "Old", "key");
            when(merchantQueryRepo.findMerchantById(1L)).thenReturn(Uni.createFrom().item(existing));
            when(userQueryService.findById(any())).thenReturn(Uni.createFrom().nullItem());
            try {
                service.updateMerchant(req).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected ResourceNotFoundException");
            } catch (ResourceNotFoundException e) {
                assertThat(e.getMessage()).contains("User not found");
            }
        }
    }

    @Nested
    @DisplayName("trashMerchant tests")
    class TrashMerchantTests {
        @Test
        void success() {
            Merchant merchant = createMerchant(1L, "Trash", "api");
            merchant.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
            when(merchantCommandRepo.trashed(1L)).thenReturn(Uni.createFrom().item(merchant));

            ApiResponse<MerchantResponseDeleteAt> resp = service.trashMerchant(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getDeletedAt()).isNotNull();
        }

        @Test
        void notFound_throwsException() {
            when(merchantCommandRepo.trashed(1L)).thenReturn(Uni.createFrom().nullItem());
            try {
                service.trashMerchant(1L).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected ResourceNotFoundException");
            } catch (ResourceNotFoundException e) {
                assertThat(e.getMessage()).contains("Merchant not found");
            }
        }
    }

    @Nested
    @DisplayName("restoreMerchant tests")
    class RestoreMerchantTests {
        @Test
        void success() {
            when(merchantCommandRepo.restore(1L))
                    .thenReturn(Uni.createFrom().item(createMerchant(1L, "Restore", "api")));
            ApiResponse<MerchantResponseDeleteAt> resp = service.restoreMerchant(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
        }

        @Test
        void notFoundOrNotTrashed_throwsException() {
            when(merchantCommandRepo.restore(1L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.restoreMerchant(1L).await().indefinitely())
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("must be trashed first");
        }
    }

    @Nested
    @DisplayName("deleteMerchant tests")
    class DeleteMerchantTests {
        @Test
        void success() {
            when(merchantCommandRepo.deletePermanent(1L)).thenReturn(Uni.createFrom().item(true));
            ApiResponse<Boolean> resp = service.deleteMerchant(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void failsIfNotTrashed_throwsException() {
            when(merchantCommandRepo.deletePermanent(1L)).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteMerchant(1L).await().indefinitely())
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("must be trashed");
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
        void noTrashed_throwsException() {
            when(merchantCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
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
        void noTrashed_throwsException() {
            when(merchantCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}