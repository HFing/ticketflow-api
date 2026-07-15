package com.hfing.ticketflowapi.event.controller;

import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.event.dto.EventResponse;
import com.hfing.ticketflowapi.event.service.EventService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ApiResponse<List<EventResponse>> getEvents() {
        var data = eventService.getPublishedUpcomingEvents();
        return ApiResponse.<List<EventResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Events retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<EventResponse> getEventById(@PathVariable String id) {
        var data = eventService.getPublicEventById(id);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Event retrieved successfully")
                .data(data)
                .build();
    }
}
