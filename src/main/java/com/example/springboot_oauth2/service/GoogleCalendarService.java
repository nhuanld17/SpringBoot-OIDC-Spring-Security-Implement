package com.example.springboot_oauth2.service;

import com.example.springboot_oauth2.response.CalendarListResponse;
import com.example.springboot_oauth2.response.EventsResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class GoogleCalendarService {
    private final RestClient restClient;

    private final String CALENDAR_LIST_URI = "https://www.googleapis.com/calendar/v3/users/me/calendarList";

    public GoogleCalendarService(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Lấy danh sách các lịch của người dùng
     * accessToken được gắn vào header "Authorization: Bearer ..."
     */
    public CalendarListResponse listCalendars(String accessToken) {
        return restClient.get()
                .uri(CALENDAR_LIST_URI)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve().body(CalendarListResponse.class);
    }

    /**
     * Lấy các sự kiện sắp tới của 1 lịch
     * Tham số query:
     * - maxResult: giới hạn số lượng sự kiện
     * - orderBy = startTime + singleEvents=true: sắp xếp thời gian
     * - timeMin = chỉ lấy sự kiện từ thời điểm timeMin trở đi
     */
    public EventsResponse listUpcomingEvents(String accessToken, String calendarId, String timeMin)  {
        URI uri = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com")
                .pathSegment("calendar", "v3", "calendars", calendarId, "events")
                .queryParam("maxResults", 10)
                .queryParam("orderBy", "startTime")
                .queryParam("singleEvents", true)
                .queryParam("timeMin", timeMin)
                .build()
                .encode()
                .toUri();

        System.out.println(">>> Events URL = " + uri);  // in ra để tự kiểm chứng

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve().body(EventsResponse.class);
    }
}
