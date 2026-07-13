package com.example.springboot_oauth2.service;

import com.example.springboot_oauth2.config.GoogleOidcProperties;
import com.example.springboot_oauth2.response.Jwks;
import com.example.springboot_oauth2.utils.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Tai JWKS cua Google va dung RSAPublicKey tu (n, e). Cache theo kid.
 * Google xoay key dinh ky -> gap kid la thi refetch.
 */
@Service
public class GoogleJwksClientService {
    private final RestClient restClient;
    private final GoogleOidcProperties props;

    private volatile Map<String, RSAPublicKey> cache = new HashMap<>();

    public GoogleJwksClientService(RestClient restClient, GoogleOidcProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    /** Tra ve public key ung voi kid; neu chua co thi tai lai JWKS 1 lan. */
    public RSAPublicKey resolve(String kid) {
        RSAPublicKey key = cache.get(kid);
        if (key != null) {
            return key;
        }
        refresh(); // kid la -> co the Google vua xoay key
        key = cache.get(kid);
        if (key == null) {
            throw new IllegalStateException("Khong tim thay public key (kid=" + kid + ")  trong JWKS Google.");
        }
        return key;
    }

    private void refresh() {
        Jwks jwks = restClient.get()
                .uri(props.jwksUri())
                .retrieve()
                .body(Jwks.class);

        Map<String, RSAPublicKey> fresh = new HashMap<>();
        if (jwks != null && jwks.keys() != null) {
            for (Jwks.Jwk jwk : jwks.keys()) {
                fresh.put(jwk.kid(), toRsaPublicKey(jwk));
            }
        }
        this.cache = fresh;
    }

    /** Dung RSAPublicKey tu modulus (n) + exponent (e) dang base64url. */
    private RSAPublicKey toRsaPublicKey(Jwks.Jwk jwk) {
        try {
            // so 1 = dau duong: n, e la so nguyen duong lon
            BigInteger modulus = new BigInteger(1, JwtUtil.base64UrlDecode(jwk.n()));
            BigInteger exponent = new BigInteger(1, JwtUtil.base64UrlDecode(jwk.e()));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(new RSAPublicKeySpec(modulus,
                    exponent));
        } catch (Exception ex) {
            throw new IllegalStateException("Khong dung duoc RSA public key tu JWK: " +
                    ex.getMessage(), ex);
        }
    }
}
