package com.example.springboot_oauth2.controller;

import com.example.springboot_oauth2.response.CalendarListResponse;
import com.example.springboot_oauth2.response.EventsResponse;
import com.example.springboot_oauth2.service.GoogleCalendarService;
import com.example.springboot_oauth2.service.TokenService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Controller
public class CalendarController {

    private final GoogleCalendarService calendarService;
    private final TokenService tokenService;

    public CalendarController(GoogleCalendarService calendarService, TokenService tokenService) {
        this.calendarService = calendarService;
        this.tokenService = tokenService;
    }

    @GetMapping("/calendars")
    public String calendars(HttpSession session, Model model) {
        // Lấy token đã lưu từ flow OAuth
        String accessToken = tokenService.getValidAccessToken(session);

        // Nếu chưa đăng nhập hoặc chưa được ủy quyền -> về trang chủ
        if (accessToken == null) {
            return "redirect:/";
        }

        try {
            CalendarListResponse calendars = calendarService.listCalendars(accessToken);
            model.addAttribute("calendars", calendars.items());
        } catch (RestClientResponseException ex) {
            model.addAttribute("error", "Google Calendar API trả về lỗi "
                    + ex.getStatusCode().value() + " khi lấy danh sách lịch.");
            return "error-page";
        }

        return "calendars";
    }

    @GetMapping("/events")
    public String events(
        @RequestParam String calendarId,
        HttpSession session,
        Model model
    ){
        String accessToken = tokenService.getValidAccessToken(session);

        if (accessToken == null) {
            return "redirect:/";
        }

        // timeMin = thời điểm hiện tại (dạng RFC3339, vd 2026-07-08T14:00:00+07:00)
        String now = Instant.now().toString();

        EventsResponse events;
        try {
            events = calendarService.listUpcomingEvents(accessToken, calendarId, now);
        } catch (RestClientResponseException ex) {
            model.addAttribute("error", "Google Calendar API trả về lỗi "
                    + ex.getStatusCode().value()
                    + " khi lấy sự kiện của lịch: " + calendarId);
            return "error-page";
        }

        model.addAttribute("calendarId", calendarId);
        model.addAttribute("events", events.items());

        return "events";
    }
}
