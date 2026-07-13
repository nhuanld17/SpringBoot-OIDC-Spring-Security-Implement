package com.example.springboot_oauth2.model;

/**
 * Danh tính người dùng trích từ ID token đã verify. Đây là "kết quả đăng nhập" của OIDC.
 */
public record AuthenticatedUser(
        String sub,           // subject: ID ổn định, duy nhất của user tại Google
        String email,
        boolean emailVerified,
        String name,
        String picture
) {
}
