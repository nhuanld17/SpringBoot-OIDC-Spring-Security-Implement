package com.example.springboot_oauth2.response;

import java.util.List;

/**
 * JWKS = JSON Web Key Set. Google publish tai jwks-uri, chua cac public key de verify
 chu ky.
 * Vi Spring dung Jackson (FAIL_ON_UNKNOWN=false), cac field thua trong JSON se bi bo
 qua.
 */
public record Jwks(
        List<Jwk> keys
) {
    public record Jwk(
            String kid,  // key id - khop voi "kid" trong header ID token
            String kty,  // key type = "RSA"
            String alg,  // "RS256"
            String use,  // "sig"
            String n,    // modulus (base64url)
            String e     // exponent (base64url)
    ) {
    }
}


