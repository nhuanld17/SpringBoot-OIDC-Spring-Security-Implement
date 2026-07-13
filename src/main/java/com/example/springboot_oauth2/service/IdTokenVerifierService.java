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
 * Verify ID token dung tinh than OIDC:
 *   1) Chu ky RS256 bang public key Google (JWKS)
 *   2) Cac claim bat buoc: iss, aud, exp, iat, nonce
 * Hop le -> tra AuthenticatedUser. Sai -> nem exception (controller bat lai).
 */
@Service
public class IdTokenVerifierService {

    private static final long CLOCK_SKEW_SECONDS = 60; // du sai lech dong ho
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

        // 1) Thuat toan + verify chu ky ------------------------------------
        String alg = text(header, "alg");
        if (!"RS256".equals(alg)) {
            throw new IllegalStateException("alg khong phai RS256: " + alg);
        }
        String kid = text(header, "kid");
        if (kid == null) {
            throw new IllegalStateException("Header ID token thieu 'kid'.");
        }

        RSAPublicKey publicKey = jwksClientService.resolve(kid);
        String signingInput = headerB64 + "." + payloadB64; // dung phan nay duoc ky
        if (!verifySignature(signingInput, signatureB64, publicKey)) {
            throw new IllegalStateException("Chu ky ID token khong hop le (co the bi gia  mao).");
        }

        // 2) Kiem tra claims ----------------------------------------------
        String iss = text(claims, "iss");
        if (iss == null || !VALID_ISSUERS.contains(iss)) {
            throw new IllegalStateException("iss khong hop le: " + iss);
        }

        String aud = text(claims, "aud");
        if (aud == null || !aud.equals(props.clientId())) {
            throw new IllegalStateException("aud khong khop client_id cua ung dung.");
        }

        long now = Instant.now().getEpochSecond();
        long exp = claims.path("exp").asLong(0);
        if (exp + CLOCK_SKEW_SECONDS < now) {
            throw new IllegalStateException("ID token da het han.");
        }
        long iat = claims.path("iat").asLong(0);
        if (iat - CLOCK_SKEW_SECONDS > now) {
            throw new IllegalStateException("ID token co iat trong tuong lai.");
        }

        String nonce = text(claims, "nonce");
        if (nonce == null || !nonce.equals(expectedNonce)) {
            throw new IllegalStateException("nonce khong khop -> nghi ngo replay. Tu choi.");
        }

        // 3) Trich thong tin user -----------------------------------------
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
            throw new IllegalStateException("Loi khi verify chu ky: " + e.getMessage(),
                    e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
