package com.example.springboot_oauth2.response;

import java.util.List;

/**
 * JWKS = JSON Web Key Set. Google publish tại jwks-uri, chứa các public key để verify chữ ký.
 * Vì Spring dùng Jackson (FAIL_ON_UNKNOWN=false), các field thừa trong JSON sẽ bị bỏ qua.
 */
public record Jwks(
        List<Jwk> keys
) {
    public record Jwk(
            String kid,  // key id - khớp với "kid" trong header ID token
            String kty,  // key type = "RSA"
            String alg,  // "RS256"
            String use,  // "sig"
            String n,    // modulus (base64url)
            String e     // exponent (base64url)
    ) {
    }
}


