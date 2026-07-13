package com.example.springboot_oauth2.service;

import com.example.springboot_oauth2.config.GoogleOidcProperties;
import com.example.springboot_oauth2.model.AuthenticatedUser;
import com.example.springboot_oauth2.utils.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Set;

/**
 * Verify ID token đúng tinh thần OIDC:
 *   1) Chữ ký RS256 bằng public key Google (JWKS)
 *   2) Các claim bắt buộc: iss, aud, exp, iat, nonce
 * Hợp lệ -> trả AuthenticatedUser. Sai -> ném exception (controller bắt lại).
 */
@Service
public class IdTokenVerifierService {

    private static final long CLOCK_SKEW_SECONDS = 60; // bù sai lệch đồng hồ
    private static final Set<String> VALID_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final GoogleJwksClientService jwksClientService;
    private final GoogleOidcProperties props;


    public IdTokenVerifierService(GoogleJwksClientService jwksClientService, GoogleOidcProperties props) {
        this.jwksClientService = jwksClientService;
        this.props = props;
    }

    public AuthenticatedUser verify(String idToken, String expectedNonce) {
        String[] parts = JwtUtil.split(idToken);
        String headerB64 = parts[0];
        String payloadB64 = parts[1];
        String signatureB64 = parts[2];

        JsonNode header = JwtUtil.decodeToJson(headerB64);
        JsonNode claims = JwtUtil.decodeToJson(payloadB64);

        // 1) Thuật toán + verify chữ ký ------------------------------------
        String alg = text(header, "alg");
        if (!"RS256".equals(alg)) {
            throw new IllegalStateException("alg không phải RS256: " + alg);
        }
        String kid = text(header, "kid");
        if (kid == null) {
            throw new IllegalStateException("Header ID token thiếu 'kid'.");
        }

        RSAPublicKey publicKey = jwksClientService.resolve(kid);
        String signingInput = headerB64 + "." + payloadB64; // đúng phần này được ký
        if (!verifySignature(signingInput, signatureB64, publicKey)) {
            throw new IllegalStateException("Chữ ký ID token không hợp lệ (có thể bị giả mạo).");
        }

        // 2) Kiểm tra claims ----------------------------------------------
        String iss = text(claims, "iss");
        if (iss == null || !VALID_ISSUERS.contains(iss)) {
            throw new IllegalStateException("iss không hợp lệ: " + iss);
        }

        String aud = text(claims, "aud");
        if (aud == null || !aud.equals(props.clientId())) {
            throw new IllegalStateException("aud không khớp client_id của ứng dụng.");
        }

        long now = Instant.now().getEpochSecond();
        long exp = claims.path("exp").asLong(0);
        if (exp + CLOCK_SKEW_SECONDS < now) {
            throw new IllegalStateException("ID token đã hết hạn.");
        }
        long iat = claims.path("iat").asLong(0);
        if (iat - CLOCK_SKEW_SECONDS > now) {
            throw new IllegalStateException("ID token có iat trong tương lai.");
        }

        String nonce = text(claims, "nonce");
        if (nonce == null || !nonce.equals(expectedNonce)) {
            throw new IllegalStateException("nonce không khớp -> nghi ngờ replay. Từ chối.");
        }

        // 3) Trích thông tin user -----------------------------------------
        return new AuthenticatedUser(
                text(claims, "sub"),
                text(claims, "email"),
                claims.path("email_verified").asBoolean(false),
                text(claims, "name"),
                text(claims, "picture")
        );
    }

    private boolean verifySignature(String signingInput, String signatureB64,
                                    RSAPublicKey key) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA"); // = RS256
            sig.initVerify(key);
            sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return sig.verify(JwtUtil.base64UrlDecode(signatureB64));
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi verify chữ ký: " + e.getMessage(),
                    e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
