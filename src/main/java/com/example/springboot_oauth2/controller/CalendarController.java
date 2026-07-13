package com.example.springboot_oauth2.controller;

import com.example.springboot_oauth2.response.CalendarListResponse;
import com.example.springboot_oauth2.response.EventsResponse;
import com.example.springboot_oauth2.service.GoogleCalendarService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Controller
public class CalendarController {

    private final GoogleCalendarService calendarService;

    public CalendarController(GoogleCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/calendars")
    public String calendars(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            Model model
    ) {
        // Spring quản lý token trong OAuth2AuthorizedClient và tự refresh nếu hết hạn
        String accessToken = client.getAccessToken().getTokenValue();

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
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            Model model
    ) {
        String accessToken = client.getAccessToken().getTokenValue();

        // timeMin = thời điểm hiện tại (dạng RFC3339 UTC, kết thúc bằng 'Z')
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
