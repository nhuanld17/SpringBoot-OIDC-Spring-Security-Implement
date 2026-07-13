package com.example.springboot_oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PHASE 1: Vì ta TỰ dựng OAuth2 flow bằng tay, ta KHÔNG dùng tính năng đăng nhập
 * của Spring Security. Nhưng spring-boot-starter-oauth2-client kéo theo Spring Security,
 * mà mặc định Spring Security khóa TẤT CẢ request -> bắt đăng nhập.
 *
 * Nên ở đây ta mở hết (permitAll) để flow thủ công hoạt động.
 * Sang Phase 2 ta sẽ thay config này bằng cấu hình OAuth2 thật sự của Spring Security.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // Demo: tắt CSRF cho đơn giản (flow của ta chỉ dùng GET redirect).
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
