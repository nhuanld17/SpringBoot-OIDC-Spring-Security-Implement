package com.example.springboot_oauth2.controller;

import com.example.springboot_oauth2.model.AuthenticatedUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttribute(OidcAuthController.SESSION_USER);

        // Guard: chua dang nhap OIDC -> ve trang chu (giong pattern CalendarController)
        if (user == null) {
            return "redirect:/";
        }

        model.addAttribute("user", user);
        return "profile";
    }
}
