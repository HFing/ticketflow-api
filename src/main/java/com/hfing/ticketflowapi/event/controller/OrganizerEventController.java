package com.hfing.ticketflowapi.event.controller;

import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.common.validation.ControllerInputValidator;
import com.hfing.ticketflowapi.event.dto.request.CreateEventRequest;
import com.hfing.ticketflowapi.event.dto.request.CreateEventShowRequest;
import com.hfing.ticketflowapi.event.dto.request.CreateTicketTypeRequest;
import com.hfing.ticketflowapi.event.dto.request.UpdateEventRequest;
import com.hfing.ticketflowapi.event.dto.response.EventResponse;
import com.hfing.ticketflowapi.event.dto.response.EventShowResponse;
import com.hfing.ticketflowapi.event.dto.response.TicketTypeResponse;
import com.hfing.ticketflowapi.event.service.IEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizer/events")
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class OrganizerEventController {

        private final IEventService eventService;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ApiResponse<EventResponse> createEvent(
                        @RequestPart("event") @Valid CreateEventRequest request,
                        @RequestPart("shortImage") MultipartFile shortImage,
                        @RequestPart("bannerImage") MultipartFile bannerImage) {
                var validatedRequest = ControllerInputValidator.requireRequestBody(request);
                var data = eventService.createEvent(
                                validatedRequest,
                                shortImage,
                                bannerImage);

                return ApiResponse.<EventResponse>builder()
                                .code(HttpStatus.CREATED.value())
                                .message("Event and images created successfully as DRAFT")
                                .data(data)
                                .build();
        }

        @PutMapping("/{id}")
        public ApiResponse<EventResponse> updateEvent(
                        @PathVariable String id,
                        @RequestBody @Valid UpdateEventRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                var userId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
                var validatedRequest = ControllerInputValidator.requireRequestBody(request);
                var role = jwt.getClaimAsString("roles");
                var data = eventService.updateEvent(id, validatedRequest, userId, role);
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
                var userId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
                var role = jwt.getClaimAsString("roles");
                eventService.deleteEvent(id, userId, role);
                return ApiResponse.<Void>builder()
                                .code(HttpStatus.OK.value())
                                .message("Event deleted successfully")
                                .build();
        }

        @PatchMapping("/{id}/cancel")
        public ApiResponse<EventResponse> cancelEvent(
                        @PathVariable String id,
                        @AuthenticationPrincipal Jwt jwt) {
                var userId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
                var role = jwt.getClaimAsString("roles");
                var data = eventService.cancelEvent(id, userId, role);
                return ApiResponse.<EventResponse>builder()
                                .code(HttpStatus.OK.value())
                                .message("Event cancelled successfully")
                                .data(data)
                                .build();
        }

        @PostMapping("/{eventId}/shows")
        public ApiResponse<EventShowResponse> createShow(
                        @PathVariable String eventId,
                        @RequestBody @Valid CreateEventShowRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                var userId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
                var validatedRequest = ControllerInputValidator.requireRequestBody(request);
                var role = jwt.getClaimAsString("roles");
                var data = eventService.createShow(eventId, validatedRequest, userId, role);
                return ApiResponse.<EventShowResponse>builder()
                                .code(HttpStatus.CREATED.value())
                                .message("Event show created successfully")
                                .data(data)
                                .build();
        }

        @PostMapping("/shows/{showId}/ticket-types")
        public ApiResponse<TicketTypeResponse> createTicketType(
                        @PathVariable String showId,
                        @RequestBody @Valid CreateTicketTypeRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
                var validatedRequest = ControllerInputValidator.requireRequestBody(request);
                String role = jwt.getClaimAsString("roles");
                var data = eventService.createTicketType(showId, validatedRequest, userId, role);
                return ApiResponse.<TicketTypeResponse>builder()
                                .code(HttpStatus.CREATED.value())
                                .message("Ticket type created successfully")
                                .data(data)
                                .build();
        }

        @PatchMapping("/{eventId}/submit-review")
        public ApiResponse<EventResponse> submitReview(
                        @PathVariable String eventId,
                        @AuthenticationPrincipal Jwt jwt) {
                var userId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
                var role = jwt.getClaimAsString("roles");
                var data = eventService.submitForReview(eventId, userId, role);
                return ApiResponse.<EventResponse>builder()
                                .code(HttpStatus.OK.value())
                                .message("Event submitted for review successfully")
                                .data(data)
                                .build();
        }
}
