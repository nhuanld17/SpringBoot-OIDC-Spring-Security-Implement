package com.example.springboot_oauth2.service;

import com.example.springboot_oauth2.config.GoogleOidcProperties;
import com.example.springboot_oauth2.response.TokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class GoogleOidcService {

    private final RestClient restClient;
    private final GoogleOidcProperties props;

    public GoogleOidcService(RestClient restClient, GoogleOidcProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    public TokenResponse exchangeCodeForToken(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", props.redirectUri());
        form.add("code_verifier", codeVerifier);
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());

        return restClient.post()
                .uri(props.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }

    /**
     * Dùng refreshToken để xin access token mới (không cần user consent lại)
     */
    public TokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());

        return restClient.post()
                .uri(props.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }
}

