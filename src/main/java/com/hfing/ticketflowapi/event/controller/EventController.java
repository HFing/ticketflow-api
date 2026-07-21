package com.hfing.ticketflowapi.event.controller;

import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventSummaryResponse;
import com.hfing.ticketflowapi.event.service.IEventService;
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

    private final IEventService eventService;

    @GetMapping
    public ApiResponse<List<PublicEventSummaryResponse>> getEvents() {
        var data = eventService.getPublishedUpcomingEvents();
        return ApiResponse.<List<PublicEventSummaryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Events retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicEventResponse> getEventById(@PathVariable String id) {
        var data = eventService.getPublicEventById(id);
        return ApiResponse.<PublicEventResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Event retrieved successfully")
                .data(data)
                .build();
    }
}
