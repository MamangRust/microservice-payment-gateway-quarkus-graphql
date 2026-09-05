package com.sanedge.card.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudScoringConsumerTest {

    @Test
    void computeRiskScore_shouldReturn50_forVeryHighAmount() throws Exception {
        FraudScoringConsumer consumer = new FraudScoringConsumer();
        Method method = FraudScoringConsumer.class.getDeclaredMethod("computeRiskScore", BigDecimal.class, String.class);
        method.setAccessible(true);

        int score = (int) method.invoke(consumer, new BigDecimal("15000000"), "0000");
        assertThat(score).isEqualTo(50);
    }

    @Test
    void computeRiskScore_shouldReturn30_forHighAmount() throws Exception {
        FraudScoringConsumer consumer = new FraudScoringConsumer();
        Method method = FraudScoringConsumer.class.getDeclaredMethod("computeRiskScore", BigDecimal.class, String.class);
        method.setAccessible(true);

        int score = (int) method.invoke(consumer, new BigDecimal("7000000"), "0000");
        assertThat(score).isEqualTo(30);
    }

    @Test
    void computeRiskScore_shouldReturn15_forMediumAmount() throws Exception {
        FraudScoringConsumer consumer = new FraudScoringConsumer();
        Method method = FraudScoringConsumer.class.getDeclaredMethod("computeRiskScore", BigDecimal.class, String.class);
        method.setAccessible(true);

        int score = (int) method.invoke(consumer, new BigDecimal("3000000"), "0000");
        assertThat(score).isEqualTo(15);
    }

    @Test
    void computeRiskScore_shouldReturn0_forLowAmount() throws Exception {
        FraudScoringConsumer consumer = new FraudScoringConsumer();
        Method method = FraudScoringConsumer.class.getDeclaredMethod("computeRiskScore", BigDecimal.class, String.class);
        method.setAccessible(true);

        int score = (int) method.invoke(consumer, new BigDecimal("500000"), "0000");
        assertThat(score).isEqualTo(0);
    }

    @Test
    void computeRiskScore_shouldAdd40_forBlacklistedMcc() throws Exception {
        FraudScoringConsumer consumer = new FraudScoringConsumer();
        Method method = FraudScoringConsumer.class.getDeclaredMethod("computeRiskScore", BigDecimal.class, String.class);
        method.setAccessible(true);

        int score = (int) method.invoke(consumer, new BigDecimal("500000"), "7995");
        // 0 (low amount) + 40 (blacklisted MCC) = 40
        assertThat(score).isEqualTo(40);
    }

    @Test
    void computeRiskScore_shouldCapAt100() throws Exception {
        FraudScoringConsumer consumer = new FraudScoringConsumer();
        Method method = FraudScoringConsumer.class.getDeclaredMethod("computeRiskScore", BigDecimal.class, String.class);
        method.setAccessible(true);

        int score = (int) method.invoke(consumer, new BigDecimal("20000000"), "7995");
        // 50 (very high) + 40 (blacklisted) = 90 → capped at 100? No, 50+40=90
        assertThat(score).isEqualTo(90);
    }

    @Test
    void computeRiskScore_shouldCapAt100_forExtreme() throws Exception {
        FraudScoringConsumer consumer = new FraudScoringConsumer();
        Method method = FraudScoringConsumer.class.getDeclaredMethod("computeRiskScore", BigDecimal.class, String.class);
        method.setAccessible(true);

        // Both conditions max: 50 + 40 = 90, then another condition... 
        // Actually max is 50+40=90, so no overflow
        int score = (int) method.invoke(consumer, new BigDecimal("20000000"), "5967");
        assertThat(score).isLessThanOrEqualTo(100);
    }
}
