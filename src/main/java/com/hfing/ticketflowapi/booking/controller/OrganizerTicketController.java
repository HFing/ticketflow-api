package com.hfing.ticketflowapi.booking.controller;

import com.hfing.ticketflowapi.booking.dto.request.CheckInTicketRequest;
import com.hfing.ticketflowapi.booking.dto.response.OrganizerTicketResponse;
import com.hfing.ticketflowapi.booking.service.IOrganizerTicketService;
import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.common.response.PageResponse;
import com.hfing.ticketflowapi.common.validation.ControllerInputValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizer")
@PreAuthorize("hasRole('ORGANIZER')")
public class OrganizerTicketController {
    private final IOrganizerTicketService organizerTicketService;

    @PostMapping("/tickets/check-in")
    public ApiResponse<OrganizerTicketResponse> checkIn(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CheckInTicketRequest request
    ) {
        String organizerId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        CheckInTicketRequest validatedRequest = ControllerInputValidator.requireRequestBody(request);
        OrganizerTicketResponse data = organizerTicketService.checkIn(organizerId, validatedRequest);
        return ApiResponse.<OrganizerTicketResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Ticket checked in successfully")
                .data(data)
                .build();
    }

    @GetMapping("/event-shows/{showId}/tickets")
    public ApiResponse<PageResponse<OrganizerTicketResponse>> getTicketsByEventShow(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String showId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        String organizerId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        PageResponse<OrganizerTicketResponse> data = PageResponse.from(
                organizerTicketService.getTicketsByEventShow(organizerId, showId, pageable));
        return ApiResponse.<PageResponse<OrganizerTicketResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Event show tickets retrieved successfully")
                .data(data)
                .build();
    }
}
