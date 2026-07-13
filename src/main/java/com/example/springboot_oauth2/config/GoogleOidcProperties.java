package com.example.springboot_oauth2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.oidc")
public record GoogleOidcProperties (
        String clientId,
        String clientSecret,
        String redirectUri,
        String scope,
        String authorizationUri,
        String tokenUri,
        String issuer,
        String jwksUri
){
    /**
     * Scope mặc định cho OIDC + Calendar API
     * openid profile email: OIDC claims
     * https://www.googleapis.com/auth/calendar.readonly: Calendar API
     */
    public static final String DEFAULT_SCOPE = "openid profile email https://www.googleapis.com/auth/calendar.readonly";
}
