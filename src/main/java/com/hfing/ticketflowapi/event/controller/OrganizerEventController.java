package com.hfing.ticketflowapi.event.controller;

import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.event.dto.CreateEventRequest;
import com.hfing.ticketflowapi.event.dto.EventResponse;
import com.hfing.ticketflowapi.event.dto.UpdateEventRequest;
import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizer/events")
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class OrganizerEventController {

    private final EventService eventService;

    @PostMapping
    public ApiResponse<EventResponse> createEvent(
            @RequestBody @Valid CreateEventRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        var data = eventService.createEvent(request, userId);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Event created successfully as DRAFT")
                .data(data)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<EventResponse> updateEvent(
            @PathVariable String id,
            @RequestBody @Valid UpdateEventRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("roles");
        var data = eventService.updateEvent(id, request, userId, role);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Event updated successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteEvent(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("roles");
        eventService.deleteEvent(id, userId, role);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Event deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/publish")
    public ApiResponse<EventResponse> publishEvent(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("roles");
        var data = eventService.publishEvent(id, userId, role);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Event published successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<EventResponse> cancelEvent(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("roles");
        var data = eventService.cancelEvent(id, userId, role);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Event cancelled successfully")
                .data(data)
                .build();
    }
}