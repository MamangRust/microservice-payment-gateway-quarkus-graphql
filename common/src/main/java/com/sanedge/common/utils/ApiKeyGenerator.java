package com.sanedge.common.utils;

import java.security.SecureRandom;

public class ApiKeyGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateApiKey() {
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);

        StringBuilder hex = new StringBuilder();
        for (byte b : key) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
}
