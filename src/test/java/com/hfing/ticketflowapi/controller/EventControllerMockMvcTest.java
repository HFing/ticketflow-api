package com.hfing.ticketflowapi.controller;

import com.hfing.ticketflowapi.dto.response.EventResponse;
import com.hfing.ticketflowapi.dto.response.EventShowResponse;
import com.hfing.ticketflowapi.common.enums.EventStatus;
import com.hfing.ticketflowapi.common.enums.EventCategory;
import com.hfing.ticketflowapi.exception.GlobalExceptionHandler;
import com.hfing.ticketflowapi.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

class EventControllerMockMvcTest {

    private MockMvc mockMvc;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getEvents_whenNoAuth_returnsEvents() throws Exception {
        var show = new EventShowResponse("show-1", LocalDateTime.now(), LocalDateTime.now().plusHours(2));
        var event = new EventResponse(
                "event-1",
                "Concert",
                "Rock concert",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                "Stadium",
                EventStatus.PUBLISHED,
                "organizer-1",
                "http://banner.com",
                "http://short.com",
                EventCategory.LIVE_MUSIC,
                List.of(show),
                Instant.now(),
                Instant.now()
        );
        when(eventService.getEvents(null, null)).thenReturn(List.of(event));

        mockMvc.perform(get("/api/events")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Events retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value("event-1"))
                .andExpect(jsonPath("$.data[0].name").value("Concert"));
    }
}
