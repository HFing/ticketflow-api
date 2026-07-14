package com.hfing.ticketflowapi.event.mapper;

import com.hfing.ticketflowapi.event.dto.CreateEventRequest;
import com.hfing.ticketflowapi.event.dto.EventResponse;
import com.hfing.ticketflowapi.event.dto.UpdateEventRequest;
import com.hfing.ticketflowapi.event.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;


@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {EventShowMapper.class}
)
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "shows", ignore = true)
    Event toEvent(CreateEventRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "shows", ignore = true)
    void updateEventFromRequest(UpdateEventRequest request, @MappingTarget Event event);

    @Mapping(target = "organizerId", source = "organizer.id")
    EventResponse toEventResponse(Event event);
}