package com.hfing.ticketflowapi.event.controller;

import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.event.dto.EventResponse;
import com.hfing.ticketflowapi.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final EventService eventService;

    @GetMapping("/pending")
    public ApiResponse<List<EventResponse>> getPendingEvents() {
        var data = eventService.getPendingEvents();
        return ApiResponse.<List<EventResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Pending events retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse> getEventById(
            @PathVariable String eventId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("roles");
        var data = eventService.getEventById(eventId);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Event details retrieved successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{eventId}/approve")
    public ApiResponse<EventResponse> approveEvent(@PathVariable String eventId) {
        var data = eventService.approveEvent(eventId);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Event approved successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{eventId}/reject")
    public ApiResponse<EventResponse> rejectEvent(@PathVariable String eventId) {
        var data = eventService.rejectEvent(eventId);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Event rejected successfully")
                .data(data)
                .build();
    }
}
