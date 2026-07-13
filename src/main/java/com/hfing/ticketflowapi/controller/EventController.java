package com.hfing.ticketflowapi.controller;

import com.hfing.ticketflowapi.dto.response.ApiResponse;
import com.hfing.ticketflowapi.dto.response.EventResponse;
import com.hfing.ticketflowapi.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ApiResponse<List<EventResponse>> getEvents(Authentication authentication) {
        String userId = null;
        String role = null;
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                userId = jwt.getSubject();
                role = jwt.getClaimAsString("roles");
            }
        }
        var data = eventService.getEvents(userId, role);
        return ApiResponse.<List<EventResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Events retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<EventResponse> getEventById(@PathVariable String id, Authentication authentication) {
        String userId = null;
        String role = null;
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                userId = jwt.getSubject();
                role = jwt.getClaimAsString("roles");
            }
        }
        var data = eventService.getEventById(id, userId, role);
        return ApiResponse.<EventResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Event retrieved successfully")
                .data(data)
                .build();
    }
}
