package com.example.springboot_oauth2.utils;

import java.security.SecureRandom;
import java.util.Base64;

public final class NonceUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * Sinh "nonce" ngẫu nhiên cho OIDC.
     * Khác state (chống CSRF ở callback): nonce sẽ được Google NHÚNG vào ID token.
     * Lúc verify, ta so nonce trong token với nonce đã lưu -> chống REPLAY id_token.
     */
    public static String generateNonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }
}
