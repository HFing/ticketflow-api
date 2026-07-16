package com.hfing.ticketflowapi.event.controller;

import com.hfing.ticketflowapi.common.exception.GlobalExceptionHandler;
import com.hfing.ticketflowapi.event.dto.response.PublicEventSummaryResponse;
import com.hfing.ticketflowapi.event.enums.EventCategory;
import com.hfing.ticketflowapi.event.service.EventService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void getEvents_whenNoAuth_returnsPublicEvents() throws Exception {
        var event = new PublicEventSummaryResponse(
                "event-1",
                "Concert",
                "Rock concert",
                "Stadium",
                "TMA Hall",
                "Organizer Event",
                "http://banner.com",
                "http://short.com",
                true,
                EventCategory.LIVE_MUSIC,
                BigDecimal.valueOf(100000),
                Instant.parse("2026-05-05T17:00:00Z")
        );
        when(eventService.getPublishedUpcomingEvents()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/events")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Events retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value("event-1"))
                .andExpect(jsonPath("$.data[0].name").value("Concert"))
                .andExpect(jsonPath("$.data[0].venue").value("TMA Hall"))
                .andExpect(jsonPath("$.data[0].isHot").value(true))
                .andExpect(jsonPath("$.data[0].organizerName").value("Organizer Event"))
                .andExpect(jsonPath("$.data[0].minPrice").value(100000))
                .andExpect(jsonPath("$.data[0].day").value("2026-05-05T17:00:00Z"))
                .andExpect(jsonPath("$.data[0].shows").doesNotExist())
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].organizerId").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist());
    }
}
