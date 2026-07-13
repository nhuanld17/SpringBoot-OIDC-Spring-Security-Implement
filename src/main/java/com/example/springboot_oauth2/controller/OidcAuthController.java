package com.example.springboot_oauth2.controller;

import com.example.springboot_oauth2.config.GoogleOidcProperties;
import com.example.springboot_oauth2.model.AuthenticatedUser;
import com.example.springboot_oauth2.response.TokenResponse;
import com.example.springboot_oauth2.service.GoogleOidcService;
import com.example.springboot_oauth2.service.IdTokenVerifierService;
import com.example.springboot_oauth2.service.TokenService;
import com.example.springboot_oauth2.utils.NonceUtils;
import com.example.springboot_oauth2.utils.PkceUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

@Controller
public class OidcAuthController {

    private final GoogleOidcProperties props;
    private final GoogleOidcService oidcService;
    private final IdTokenVerifierService idTokenVerifierService;
    private final TokenService tokenService;

    // Session keys RIÊNG cho OIDC (tách khỏi nhánh Calendar dùng "oauth_*")
    public static final String SESSION_STATE = "oidc_state";
    public static final String SESSION_NONCE = "oidc_nonce";
    public static final String SESSION_CODE_VERIFIER = "oidc_code_verifier";
    public static final String SESSION_USER = "oidc_user";
    public static final String SESSION_TOKEN = "oidc_token";
    public static final String SESSION_EXPIRES_AT = "oidc_expires_at";

    public OidcAuthController(GoogleOidcProperties props,
                              GoogleOidcService oidcService,
                              IdTokenVerifierService idTokenVerifierService,
                              TokenService tokenService) {
        this.props = props;
        this.oidcService = oidcService;
        this.idTokenVerifierService = idTokenVerifierService;
        this.tokenService = tokenService;
    }

    @GetMapping("/oidc/authorize")
    public String authorize(HttpSession session) {
        String state = PkceUtil.generateState();
        String nonce = NonceUtils.generateNonce();
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);

        session.setAttribute(SESSION_STATE, state);
        session.setAttribute(SESSION_NONCE, nonce);
        session.setAttribute(SESSION_CODE_VERIFIER, codeVerifier);

        String authorizeUrl = UriComponentsBuilder
                .fromUriString(props.authorizationUri())
                .queryParam("client_id", props.clientId())
                .queryParam("redirect_uri", props.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", props.scope())            // openid profile email + calendar
                .queryParam("state", state)                    // chống CSRF
                .queryParam("nonce", nonce)                    // chống replay ID token
                .queryParam("code_challenge", codeChallenge)   // PKCE
                .queryParam("code_challenge_method", "S256")
                .queryParam("access_type", "offline")           // xin cả REFRESH token
                .queryParam("prompt", "consent")                // luôn hiện consent (để chắc chắn có refresh token)
                .build()
                .encode()
                .toUriString();

        return "redirect:" + authorizeUrl;
    }

    @GetMapping("/oidc/callback")
    public String callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("error", "Google trả về lỗi: " + error);
            return "error-page";
        }

        // Chống CSRF: state phải khớp
        String savedState = (String) session.getAttribute(SESSION_STATE);
        if (savedState == null || !savedState.equals(state)) {
            model.addAttribute("error", "State không khớp -> nghi ngờ CSRF. Từ chối.");
            return "error-page";
        }

        String codeVerifier = (String) session.getAttribute(SESSION_CODE_VERIFIER);
        String expectedNonce = (String) session.getAttribute(SESSION_NONCE);

        // Đổi code lấy token
        TokenResponse token;
        try {
            token = oidcService.exchangeCodeForToken(code, codeVerifier);
        } catch (RestClientResponseException ex) {
            model.addAttribute("error", "Đổi code lấy token thất bại ("
                    + ex.getStatusCode().value() + "): " +
                    ex.getResponseBodyAsString());
            return "error-page";
        }

        if (token == null || token.id_token() == null) {
            model.addAttribute("error", "Không nhận được id_token từ Google.");
            return "error-page";
        }

        // VERIFY id_token (chữ ký + claims + nonce)
        AuthenticatedUser user;
        try {
            user = idTokenVerifierService.verify(token.id_token(), expectedNonce);
        } catch (RuntimeException ex) {
            model.addAttribute("error", "ID token không hợp lệ: " + ex.getMessage());
            return "error-page";
        }

        // Đăng nhập thành công -> lưu user + token, dọn giá trị dùng 1 lần
        session.setAttribute(SESSION_USER, user);
        session.setAttribute(SESSION_TOKEN, token);

        // Tính thời điểm access token hết hạn = bây giờ + expires_in (second)
        Instant expiresAt = Instant.now().plusSeconds(token.expires_in());
        session.setAttribute(SESSION_EXPIRES_AT, expiresAt);

        session.removeAttribute(SESSION_STATE);
        session.removeAttribute(SESSION_NONCE);
        session.removeAttribute(SESSION_CODE_VERIFIER);

        // Hiển thị ra connected.html
        model.addAttribute("hasAccessToken", token.access_token() != null);
        model.addAttribute("hasRefreshToken", token.refresh_token() != null);
        model.addAttribute("expiresIn", token.expires_in());
        model.addAttribute("scope", token.scope());
        model.addAttribute("tokenType", token.token_type());

        return "connected";
    }

    @GetMapping("/oidc/refresh")
    public String forceRefresh(HttpSession session, Model model) {
        TokenResponse current = (TokenResponse) session.getAttribute(SESSION_TOKEN);
        if (current == null || current.refresh_token() == null) {
            model.addAttribute("error", "Chưa có refresh token - hãy connect lại");
            return "error-page";
        }

        String oldAccess = current.access_token();
        TokenResponse refreshed;

        try {
            refreshed = tokenService.refresh(session, current);  // ép refresh access token ngay
        } catch (RestClientResponseException ex) {
            session.removeAttribute(SESSION_TOKEN);
            session.removeAttribute(SESSION_EXPIRES_AT);
            session.removeAttribute(SESSION_USER);

            model.addAttribute("error",
                    "Refresh token không còn hợp lệ (có thể đã bị thu hồi). Hãy connect lại.");

            return "error-page";
        }

        // So sánh 20 ký tự đầu để thấy token đã ĐỔI
        model.addAttribute("oldPrefix", oldAccess.substring(0, Math.min(20, oldAccess.length())) + "...");
        model.addAttribute("newPrefix", refreshed.access_token().substring(0, Math.min(20, refreshed.access_token().length())) + "...");
        model.addAttribute("newExpiresIn", refreshed.expires_in());

        return "refreshed";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
