package com.hfing.ticketflowapi.event.mapper;

import com.hfing.ticketflowapi.event.dto.TicketTypeResponse;
import com.hfing.ticketflowapi.event.entity.TicketType;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TicketTypeMapper {
    TicketTypeResponse toTicketTypeResponse(TicketType ticketType);
}
