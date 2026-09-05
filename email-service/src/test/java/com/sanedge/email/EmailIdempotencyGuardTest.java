package com.sanedge.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class EmailIdempotencyGuardTest {

    @Mock ReactiveRedisDataSource reactiveRedis;
    @Mock ReactiveValueCommands<String, String> valueCommands;
    @Mock ReactiveKeyCommands<String> keyCommands;

    private EmailIdempotencyGuard guard;

    private static final String EVENT_ID = "evt-123";
    private static final String KEY = "email:idempotency:evt-123";

    @BeforeEach
    void setUp() {
        guard = new EmailIdempotencyGuard();
        guard.reactiveRedis = reactiveRedis;
        guard.leaseSeconds = 60;
        guard.ttlSeconds = 86_400;
        lenient().when(reactiveRedis.value(String.class, String.class)).thenReturn(valueCommands);
        lenient().when(reactiveRedis.key(String.class)).thenReturn(keyCommands);
    }

    @Nested
    @DisplayName("claim (SETNX + EXPIRE lease)")
    class ClaimTests {

        @Test
        void setnxSucceeds_returnsClaimed() {
            when(valueCommands.setnx(KEY, "PROCESSING")).thenReturn(Uni.createFrom().item(true));
            when(keyCommands.expire(KEY, 60)).thenReturn(Uni.createFrom().item(true));

            assertEquals(EmailIdempotencyGuard.ClaimResult.CLAIMED,
                    guard.claim(EVENT_ID).await().indefinitely());
        }

        @Test
        void setnxFailsOnSentState_returnsDuplicate() {
            when(valueCommands.setnx(KEY, "PROCESSING")).thenReturn(Uni.createFrom().item(false));
            when(valueCommands.get(KEY)).thenReturn(Uni.createFrom().item("SENT"));

            assertEquals(EmailIdempotencyGuard.ClaimResult.DUPLICATE,
                    guard.claim(EVENT_ID).await().indefinitely());
        }

        @Test
        void setnxFailsOnActiveLease_returnsBusy() {
            when(valueCommands.setnx(KEY, "PROCESSING")).thenReturn(Uni.createFrom().item(false));
            when(valueCommands.get(KEY)).thenReturn(Uni.createFrom().item("PROCESSING"));
            when(keyCommands.ttl(KEY)).thenReturn(Uni.createFrom().item(30L));

            assertEquals(EmailIdempotencyGuard.ClaimResult.BUSY,
                    guard.claim(EVENT_ID).await().indefinitely());
        }

        @Test
        void orphanedProcessingKey_isReclaimed() {
            // Crash between SETNX and EXPIRE leaves the key without TTL (ttl == -1):
            // the next claim must delete and re-claim instead of blocking forever.
            when(valueCommands.setnx(KEY, "PROCESSING"))
                    .thenReturn(Uni.createFrom().item(false), Uni.createFrom().item(true));
            when(valueCommands.get(KEY)).thenReturn(Uni.createFrom().item("PROCESSING"));
            when(keyCommands.ttl(KEY)).thenReturn(Uni.createFrom().item(-1L));
            when(keyCommands.del(KEY)).thenReturn(Uni.createFrom().item(1));
            when(keyCommands.expire(KEY, 60)).thenReturn(Uni.createFrom().item(true));

            assertEquals(EmailIdempotencyGuard.ClaimResult.CLAIMED,
                    guard.claim(EVENT_ID).await().indefinitely());
        }

        @Test
        void failOpenOnRedisError_returnsClaimed() {
            when(valueCommands.setnx(KEY, "PROCESSING"))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("redis down")));

            assertEquals(EmailIdempotencyGuard.ClaimResult.CLAIMED,
                    guard.claim(EVENT_ID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("markSent / release")
    class TerminalTests {

        @Test
        void markSent_setsSentWithTtl() {
            when(valueCommands.setex(KEY, 86_400, "SENT")).thenReturn(Uni.createFrom().voidItem());

            guard.markSent(EVENT_ID).await().indefinitely();

            verify(valueCommands).setex(KEY, 86_400, "SENT");
        }

        @Test
        void release_deletesKey() {
            when(keyCommands.del(KEY)).thenReturn(Uni.createFrom().item(1));

            guard.release(EVENT_ID).await().indefinitely();

            verify(keyCommands).del(KEY);
        }
    }
}
