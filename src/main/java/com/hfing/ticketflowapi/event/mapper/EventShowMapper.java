package com.hfing.ticketflowapi.event.mapper;

import com.hfing.ticketflowapi.event.dto.EventShowRequest;
import com.hfing.ticketflowapi.event.dto.EventShowResponse;
import com.hfing.ticketflowapi.event.entity.EventShow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;


@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {TicketTypeMapper.class}
)
public interface EventShowMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "saleStartTime", ignore = true)
    @Mapping(target = "saleEndTime", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    EventShow toEventShow(EventShowRequest request);

    EventShowResponse toEventShowResponse(EventShow eventShow);
}