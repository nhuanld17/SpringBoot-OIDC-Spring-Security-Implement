package com.example.springboot_oauth2.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Tiện ích thao tác JWT (ID token) ở mức "thô": tách phần, base64url-decode, parse JSON.
 * KHÔNG verify ở đây - verify nằm trong IdTokenVerifier.
 */
public class JwtUtil {
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtUtil() {
    }

    /** Tách "header.payload.signature" thành 3 phần. */
    public static String[] split(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("ID token không đúng định dạng JWT (cần 3 phần).");
        }
        return parts;
    }

    /** base64url-decode -> bytes thô. */
    public static byte[] base64UrlDecode(String data) {
        return URL_DECODER.decode(data);
    }

    /** Decode 1 phần (header hoặc payload) từ base64url -> cây JSON để đọc claim. */
    public static JsonNode decodeToJson(String part) {
        try {
            byte[] json = base64UrlDecode(part);
            return MAPPER.readTree(new String(json, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Không parse được phần JWT: " +
                    e.getMessage(), e);
        }
    }

}
