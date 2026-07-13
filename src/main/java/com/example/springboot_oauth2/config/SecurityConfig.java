package com.example.springboot_oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình đăng nhập OIDC bằng Spring Security OAuth2 Client.
 * Framework tự lo: khởi tạo authorization request (PKCE + state + nonce),
 * đổi code lấy token, verify ID token (chữ ký JWKS + claims), và refresh token.
 */
@Configuration
public class SecurityConfig {

    // Base URI mặc định của Spring cho endpoint khởi tạo login: /oauth2/authorization/{registrationId}
    private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error").permitAll()   // trang chủ + trang lỗi mở công khai
                        .anyRequest().authenticated())                // còn lại bắt buộc đăng nhập
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/profile", true)          // login xong -> vào thẳng /profile
                        .authorizationEndpoint(authz -> authz
                                .authorizationRequestResolver(
                                        offlineAccessResolver(clientRegistrationRepository)
                                )
                        )
                )
                .logout(logout -> logout.logoutSuccessUrl("/"));      // POST /logout (CSRF) -> về trang chủ
        return http.build();
    }

    /**
     * Ép Google cấp REFRESH token: thêm access_type=offline + prompt=consent vào
     * authorization request. Thiếu 2 tham số này thì OAuth2AuthorizedClient không có
     * refresh token và cơ chế auto-refresh của Spring sẽ vô hiệu.
     */
    private OAuth2AuthorizationRequestResolver offlineAccessResolver(ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, AUTHORIZATION_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params -> {
                    params.put("access_type", "offline");
                    params.put("prompt", "consent");
                }));
        return resolver;
    }
}
