package com.hfing.ticketflowapi.event.dto;

import com.hfing.ticketflowapi.event.enums.EventShowStatus;
import java.time.LocalDateTime;
import java.util.List;


public record EventShowResponse(
        String id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        EventShowStatus status,
        List<TicketTypeResponse> ticketTypes
) {}