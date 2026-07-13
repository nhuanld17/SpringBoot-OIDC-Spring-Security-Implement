package com.example.springboot_oauth2.model;

/**
 * Danh tinh nguoi dung trich tu ID token da verify. Day la "ket qua dang nhap" cua
 OIDC.
 */
public record AuthenticatedUser(
        String sub,           // subject: ID on dinh, duy nhat cua user tai Google
        String email,
        boolean emailVerified,
        String name,
        String picture
) {
}
