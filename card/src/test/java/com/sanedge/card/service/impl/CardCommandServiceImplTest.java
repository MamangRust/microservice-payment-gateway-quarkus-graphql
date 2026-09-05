package com.sanedge.card.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.card.domain.requests.CreateCardRequest;
import com.sanedge.card.domain.requests.UpdateCardRequest;
import com.sanedge.card.domain.response.CardResponse;
import com.sanedge.card.domain.response.CardResponseDeleteAt;
import com.sanedge.card.entity.Card;
import com.sanedge.card.entity.Outbox;
import com.sanedge.card.repository.OutboxRepository;
import com.sanedge.card.entity.Card.CardStatus;
import com.sanedge.card.repository.CardCommandRepository;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.service.KafkaService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import pb.user.UserQueryService;

@ExtendWith(MockitoExtension.class)
class CardCommandImplServiceTest {

    @Mock
    private CardCommandRepository cardCommandRepository;

    @Mock
    private CardQueryRepository cardQueryRepository;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private KafkaService kafkaService;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private Validator validator;

    private TracingMetrics tracingMetrics;

    private CardCommandImplService service;

    @BeforeEach
    void setUp() {
        tracingMetrics = mock(TracingMetrics.class, withSettings().lenient());

        service = new CardCommandImplService();
        service.cardCommandRepository = cardCommandRepository;
        service.cardQueryRepository = cardQueryRepository;
        service.userQueryService = userQueryService;
        service.kafkaService = kafkaService;
        service.outboxRepository = outboxRepository;
        service.validator = validator;
        service.tracingMetrics = tracingMetrics;

        // Lenient stubs for tracingMetrics - handle both 3-param and 4-param overloads
        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Uni<?>> s = inv.getArgument(inv.getArguments().length - 1);
            return s.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Uni<?>> s = inv.getArgument(inv.getArguments().length - 1);
            return s.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());

        // default lenient for kafka and bulk ops
        lenient().when(kafkaService.sendMessage(anyString(), anyString(), any()))
                .thenReturn(Uni.createFrom().voidItem());
        lenient().when(outboxRepository.persist(any(Outbox.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Outbox) inv.getArgument(0)));
        lenient().when(cardCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(cardCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));
    }

    private Card createMockCard(Long id) {
        Card card = new Card();
        card.setCardId(id);
        card.setCardNumber("1234-5678-9012-3456");
        card.setCardType("VISA");
        card.setCardProvider("BCA");
        card.setUserId(100);
        card.setStatus(CardStatus.ACTIVE);
        card.setCreatedAt(Instant.now());
        card.setUpdatedAt(Instant.now());
        return card;
    }

    private CreateCardRequest createCardReq() {
        CreateCardRequest req = new CreateCardRequest();
        req.setUserId(100L);
        req.setCardType("VISA");
        req.setExpireDate(LocalDate.of(2027, 12, 31));
        req.setCvv("123");
        req.setCardProvider("BCA");
        return req;
    }

    private UpdateCardRequest updateCardReq() {
        UpdateCardRequest req = new UpdateCardRequest();
        req.setCardId(1L);
        req.setUserId(100L);
        req.setCardType("MASTERCARD");
        req.setExpireDate(LocalDate.of(2028, 6, 30));
        req.setCvv("456");
        req.setCardProvider("BNI");
        return req;
    }

    @Nested
    @DisplayName("createCard tests")
    class CreateCardTests {
        @Test
        void success() {
            CreateCardRequest req = createCardReq();
            pb.user.User.ApiResponseUser userResp = pb.user.User.ApiResponseUser.newBuilder()
                    .setData(pb.user.User.UserResponse.newBuilder().setId(100).build())
                    .build();
            when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResp));
            when(cardCommandRepository.persist(any(Card.class))).thenAnswer(inv -> {
                Card c = inv.getArgument(0);
                c.setCardId(10L);
                return Uni.createFrom().item(c);
            });

            ApiResponse<CardResponse> resp = service.createCard(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getId()).isEqualTo(10L);
            assertThat(resp.data().getCardNumber()).isNotNull();
        }

        @Test
        void userNotFound_returnsError() {
            CreateCardRequest req = createCardReq();
            when(userQueryService.findById(any())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<CardResponse> resp = service.createCard(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("User not found");
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Test
        void validationFails_returnsError() {
            CreateCardRequest req = new CreateCardRequest();
            ConstraintViolation<?> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
            when(violation.getPropertyPath()).thenReturn(org.mockito.Mockito.mock(Path.class));
            when(violation.getMessage()).thenReturn("must not be null");
            Set violations = new HashSet();
            violations.add(violation);
            when(validator.validate(any())).thenReturn(violations);

            ApiResponse<CardResponse> resp = service.createCard(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Validation failed");
        }
    }

    @Nested
    @DisplayName("updateCard tests")
    class UpdateCardTests {
        @Test
        void success() {
            UpdateCardRequest req = updateCardReq();
            pb.user.User.ApiResponseUser userResp = pb.user.User.ApiResponseUser.newBuilder()
                    .setData(pb.user.User.UserResponse.newBuilder().setId(100).build())
                    .build();
            when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResp));
            when(cardQueryRepository.findById(1L)).thenReturn(Uni.createFrom().item(createMockCard(1L)));
            when(cardCommandRepository.persist(any(Card.class)))
                    .thenAnswer(inv -> Uni.createFrom().item((Card) inv.getArgument(0)));

            ApiResponse<CardResponse> resp = service.updateCard(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getCardType()).isEqualTo("MASTERCARD");
        }

        @Test
        void cardNotFound_returnsError() {
            UpdateCardRequest req = updateCardReq();
            pb.user.User.ApiResponseUser userResp = pb.user.User.ApiResponseUser.newBuilder()
                    .setData(pb.user.User.UserResponse.newBuilder().setId(100).build())
                    .build();
            when(userQueryService.findById(any())).thenReturn(Uni.createFrom().item(userResp));
            when(cardQueryRepository.findById(1L)).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<CardResponse> resp = service.updateCard(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Card not found");
        }
    }

    @Nested
    @DisplayName("trashCard tests")
    class TrashCardTests {
        @Test
        void success() {
            Card trashed = createMockCard(1L);
            trashed.setDeletedAt(Instant.now());
            when(cardCommandRepository.trashed(1L)).thenReturn(Uni.createFrom().item(trashed));

            ApiResponse<CardResponseDeleteAt> resp = service.trashCard(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getDeletedAt()).isNotNull();
        }

        @Test
        void notFound_returnsError() {
            when(cardCommandRepository.trashed(1L)).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<CardResponseDeleteAt> resp = service.trashCard(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Failed to trash card");
        }
    }

    @Nested
    @DisplayName("restoreCard tests")
    class RestoreCardTests {
        @Test
        void success() {
            when(cardCommandRepository.restore(1L)).thenReturn(Uni.createFrom().item(createMockCard(1L)));
            ApiResponse<CardResponseDeleteAt> resp = service.restoreCard(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
        }

        @Test
        void notFoundOrNotTrashed_throwsException() {
            when(cardCommandRepository.restore(1L)).thenReturn(Uni.createFrom().nullItem());
            assertThatThrownBy(() -> service.restoreCard(1L).await().indefinitely())
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("must be trashed first");
        }
    }

    @Nested
    @DisplayName("deleteCard tests")
    class DeleteCardTests {
        @Test
        void success() {
            Card deletedCard = mock(Card.class);
            when(deletedCard.isPersistent()).thenReturn(true);
            when(cardCommandRepository.deletePermanent(1L)).thenReturn(Uni.createFrom().item(deletedCard));

            ApiResponse<Boolean> resp = service.deleteCard(1L).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void notTrashed_throwsException() {
            Card transientCard = mock(Card.class);
            when(transientCard.isPersistent()).thenReturn(false);
            when(cardCommandRepository.deletePermanent(1L)).thenReturn(Uni.createFrom().item(transientCard));

            assertThatThrownBy(() -> service.deleteCard(1L).await().indefinitely())
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("must be trashed before permanent deletion");
        }
    }

    @Nested
    @DisplayName("restoreAll tests")
    class RestoreAllTests {
        @Test
        void success() {
            ApiResponse<Boolean> resp = service.restoreAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void noTrashed_throwsException() {
            when(cardCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed cards found");
        }
    }

    @Nested
    @DisplayName("deleteAll tests")
    class DeleteAllTests {
        @Test
        void success() {
            ApiResponse<Boolean> resp = service.deleteAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test
        void noTrashed_throwsException() {
            when(cardCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No trashed cards found");
        }
    }
}