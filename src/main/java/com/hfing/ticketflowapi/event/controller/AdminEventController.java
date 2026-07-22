package com.hfing.ticketflowapi.event.controller;

import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.common.response.PageResponse;
import com.hfing.ticketflowapi.event.dto.response.EventResponse;
import com.hfing.ticketflowapi.event.service.IEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final IEventService eventService;

    @GetMapping("/pending")
    public ApiResponse<PageResponse<EventResponse>> getPendingEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        var data = PageResponse.from(eventService.getPendingEvents(pageable));
        return ApiResponse.<PageResponse<EventResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Pending events retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse> getEventById(
            @PathVariable String eventId) {
        var data = eventService.getAdminEventById(eventId);
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

    @PatchMapping("/{eventId}/hot")
    public ApiResponse<EventResponse> setHotEvent(
            @PathVariable String eventId,
            @RequestParam boolean isHot) {
        var data = eventService.setHotEvent(eventId, isHot);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.OK.value())
                .message(isHot ? "Event marked as hot successfully" : "Event unmarked as hot successfully")
                .data(data)
                .build();
    }
}
