package com.example.springboot_oauth2.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Tien ich thao tac JWT (ID token) o muc "tho": tach phan, base64url-decode, parse
 JSON.
 * KHONG verify o day - verify nam trong IdTokenVerifier.
 */
public class JwtUtil {
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtUtil() {
    }

    /** Tach "header.payload.signature" thanh 3 phan. */
    public static String[] split(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("ID token khong dung dinh dang JWT (can 3  phan).");
        }
        return parts;
    }

    /** base64url-decode -> bytes tho. */
    public static byte[] base64UrlDecode(String data) {
        return URL_DECODER.decode(data);
    }

    /** Decode 1 phan (header hoac payload) tu base64url -> cay JSON de doc claim. */
    public static JsonNode decodeToJson(String part) {
        try {
            byte[] json = base64UrlDecode(part);
            return MAPPER.readTree(new String(json, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Khong parse duoc phan JWT: " +
                    e.getMessage(), e);
        }
    }

}
