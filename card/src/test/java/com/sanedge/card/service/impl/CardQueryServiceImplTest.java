package com.sanedge.card.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.card.domain.requests.FindAllCards;
import com.sanedge.card.domain.response.CardResponse;
import com.sanedge.card.domain.response.CardResponseDeleteAt;
import com.sanedge.card.entity.Card;
import com.sanedge.card.entity.Card.CardStatus;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class CardQueryServiceImplTest {

    @Mock
    private CardQueryRepository cardQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private CardQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new CardQueryServiceImpl(cardQueryRepository, redisService, objectMapper, tracingMetrics);

        // Lenient stubs to execute the supplier directly (both 3-arg and 2-arg
        // variants)
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(2);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
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

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private FindAllCards findAllReq(int page, int size, String search) {
        FindAllCards req = new FindAllCards();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    @Nested
    @DisplayName("findAll tests")
    class FindAllTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllCards req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findCards(any(FindAllCards.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockCard(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<CardResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).getCardNumber()).isEqualTo("1234-5678-9012-3456");
        }

        @Test
        void cacheHit_returnsCached() {
            FindAllCards req = findAllReq(1, 10, "");
            CardResponse cachedData = CardResponse.from(createMockCard(1L));
            ApiResponsePagination<List<CardResponse>> cached = new ApiResponsePagination<>(
                    "success", "Cards retrieved successfully", List.of(cachedData), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponsePagination<List<CardResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByActive tests")
    class FindByActiveTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllCards req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findActiveCards(any(FindAllCards.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockCard(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<CardResponseDeleteAt>> result = service.findByActive(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByTrashed tests")
    class FindByTrashedTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllCards req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findTrashedCards(any(FindAllCards.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockCard(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<CardResponseDeleteAt>> result = service.findByTrashed(req).await()
                    .indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            Long id = 1L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findCardById(anyLong()))
                    .thenReturn(Uni.createFrom().item(Optional.of(createMockCard(id))));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<CardResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getId()).isEqualTo(id);
        }

        @Test
        void notFound_throwsException() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findCardById(anyLong())).thenReturn(Uni.createFrom().item(Optional.empty()));

            try {
                service.findById(999L).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected NotFoundException");
            } catch (NotFoundException e) {
                assertThat(e.getMessage()).contains("Card not found");
            }
        }
    }

    @Nested
    @DisplayName("findByUserId tests")
    class FindByUserIdTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            Long userId = 100L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findCardByUserId(anyLong()))
                    .thenReturn(Uni.createFrom().item(Optional.of(createMockCard(1L))));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<CardResponse> result = service.findByUserId(userId).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getUserId()).isEqualTo(userId);
        }

        @Test
        void notFound_throwsException() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findCardByUserId(anyLong())).thenReturn(Uni.createFrom().item(Optional.empty()));

            try {
                service.findByUserId(999L).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected NotFoundException");
            } catch (NotFoundException e) {
                assertThat(e.getMessage()).contains("Card not found");
            }
        }
    }

    @Nested
    @DisplayName("findByCardNumber tests")
    class FindByCardNumberTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            String cardNumber = "1234-5678-9012-3456";
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findCardByCardNumber(anyString()))
                    .thenReturn(Uni.createFrom().item(Optional.of(createMockCard(1L))));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<CardResponse> result = service.findByCardNumber(cardNumber).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getCardNumber()).isEqualTo(cardNumber);
        }

        @Test
        void notFound_throwsException() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findCardByCardNumber(anyString()))
                    .thenReturn(Uni.createFrom().item(Optional.empty()));

            try {
                service.findByCardNumber("nonexistent").await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected NotFoundException");
            } catch (NotFoundException e) {
                assertThat(e.getMessage()).contains("Card not found");
            }
        }
    }
}