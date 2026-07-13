package com.example.springboot_oauth2.utils;

import java.security.SecureRandom;
import java.util.Base64;

public final class NonceUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * Sinh "nonce" ngau nhien cho OIDC.
     * Khac state (chong CSRF o callback): nonce se duoc Google NHUNG vao ID token.
     * Luc verify, ta so nonce trong token voi nonce da luu -> chong REPLAY id_token.
     */
    public static String generateNonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }
}
