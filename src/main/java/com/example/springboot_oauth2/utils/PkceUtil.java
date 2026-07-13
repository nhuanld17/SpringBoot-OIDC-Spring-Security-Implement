package com.example.springboot_oauth2.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * PKCE (Proof Key for Code Exchange) - RFC 7636.
 *
 * Ý tưởng: trước khi redirect sang Google, client tạo ra 1 chuỗi bí mật ngẫu nhiên
 * "code_verifier" (chỉ client biết), rồi gửi đi bản "băm" của nó "code_challenge".
 * Lúc đổi code lấy token, client phải chứng minh mình biết code_verifier gốc.
 *
 * -> Nếu kẻ tấn công cướp được authorization code, họ VẪN không đổi được token
 *    vì không biết code_verifier.
 */
public final class PkceUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    // Base64 URL-safe, KHÔNG padding (yêu cầu của chuẩn PKCE)
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private PkceUtil() {
    }

    /**
     * Sinh code_verifier: chuỗi ngẫu nhiên 32 byte -> base64url (~43 ký tự).
     */
    public static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    /**
     * code_challenge = BASE64URL( SHA-256( code_verifier ) ).
     * code_challenge_method = S256.
     */
    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return URL_ENCODER.encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    /**
     * Sinh giá trị "state" ngẫu nhiên để chống CSRF ở bước callback.
     */
    public static String generateState() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }
}
