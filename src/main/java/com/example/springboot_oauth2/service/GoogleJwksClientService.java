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
 * Tải JWKS của Google và dựng RSAPublicKey từ (n, e). Cache theo kid.
 * Google xoay key định kỳ -> gặp kid lạ thì refetch.
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

    /** Trả về public key ứng với kid; nếu chưa có thì tải lại JWKS 1 lần. */
    public RSAPublicKey resolve(String kid) {
        RSAPublicKey key = cache.get(kid);
        if (key != null) {
            return key;
        }
        refresh(); // kid lạ -> có thể Google vừa xoay key
        key = cache.get(kid);
        if (key == null) {
            throw new IllegalStateException("Không tìm thấy public key (kid=" + kid + ") trong JWKS Google.");
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

    /** Dựng RSAPublicKey từ modulus (n) + exponent (e) dạng base64url. */
    private RSAPublicKey toRsaPublicKey(Jwks.Jwk jwk) {
        try {
            // số 1 = dấu dương: n, e là số nguyên dương lớn
            BigInteger modulus = new BigInteger(1, JwtUtil.base64UrlDecode(jwk.n()));
            BigInteger exponent = new BigInteger(1, JwtUtil.base64UrlDecode(jwk.e()));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(new RSAPublicKeySpec(modulus,
                    exponent));
        } catch (Exception ex) {
            throw new IllegalStateException("Không dựng được RSA public key từ JWK: " +
                    ex.getMessage(), ex);
        }
    }
}
