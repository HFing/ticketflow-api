package com.hfing.ticketflowapi.event.dto.response;

import com.hfing.ticketflowapi.event.enums.EventShowSaleStatus;
import java.time.LocalDateTime;
import java.util.List;

public record PublicEventShowResponse(
        String id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        EventShowSaleStatus saleStatus,
        List<PublicTicketTypeResponse> ticketTypes
) {}
