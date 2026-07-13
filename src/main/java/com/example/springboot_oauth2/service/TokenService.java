package com.example.springboot_oauth2.service;

import com.example.springboot_oauth2.controller.OidcAuthController;
import com.example.springboot_oauth2.response.TokenResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

@Service
public class TokenService {

    private final GoogleOidcService oidcService;

    public TokenService(GoogleOidcService oidcService) {
        this.oidcService = oidcService;
    }

    /**
     * Trả về accessToken nếu còn hạn, nếu sắp hết hạn (còn < 60s) và có
     * refresh token thì tự động refresh, cập nhật lại session.
     * Trả null nếu chưa ủy quyền.
     */
    public String getValidAccessToken(HttpSession session) {
        TokenResponse token = (TokenResponse) session.getAttribute(OidcAuthController.SESSION_TOKEN);

        if (token == null || token.access_token() == null) {
            return null;
        }

        Instant expiresAt = (Instant) session.getAttribute(OidcAuthController.SESSION_EXPIRES_AT);

        // Nếu còn hạn thoải mái (> 60s) -> dùng accesstoken, không refresh
        if (expiresAt != null &&
                Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return token.access_token();
        }

        // Nếu đã hết hạn hoặc sắp hết hạn: nếu không có refreshtoken thì đành dùng
        // tạm vậy (sẽ bị 401)
        if (token.refresh_token() == null) {
            return token.access_token();
        }

        // refresh token
        System.out.println(">>> Access token sắp hết hạn -> đang refresh ...");

        try {
            return refresh(session, token).access_token();
        } catch (RestClientException e) {
            // refresh token hỏng -> xóa token, buộc đăng nhập lại
            session.removeAttribute(OidcAuthController.SESSION_TOKEN);

            session.removeAttribute(OidcAuthController.SESSION_EXPIRES_AT);
            return null; // controller sẽ redirect về "/"
        }
    }

    /**
     * Gọi refresh grant và GHI ĐÈ session. Tách riêng để endpoint demo gọi trực tiếp.
     */
    public TokenResponse refresh(HttpSession session, TokenResponse currentToken) {
        TokenResponse refreshedToken = (TokenResponse) oidcService.refreshAccessToken(currentToken.refresh_token());

        // Google KHÔNG TRẢ refresh token mới -> giữ lại cái cũ
        TokenResponse merged = new TokenResponse(
                refreshedToken.access_token(),
                refreshedToken.expires_in(),
                currentToken.refresh_token(), // Vẫn giữ refresh token cũ
                refreshedToken.scope() != null ? refreshedToken.scope() : currentToken.scope(),
                refreshedToken.token_type(),
                refreshedToken.id_token()
        );

        session.setAttribute(OidcAuthController.SESSION_TOKEN, merged);
        session.setAttribute(OidcAuthController.SESSION_EXPIRES_AT,
                Instant.now().plusSeconds(refreshedToken.expires_in()));

        return merged;
    }
}
