package com.sanedge.common.utils;

import java.util.Random;

public class CardNumberGenerator {
    private static final Random RANDOM = new Random();

    public static String randomCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
