package com.hfing.ticketflowapi.event.mapper;

import com.hfing.ticketflowapi.event.dto.request.CreateEventRequest;
import com.hfing.ticketflowapi.event.dto.response.EventResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventShowResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventSummaryResponse;
import com.hfing.ticketflowapi.event.dto.request.UpdateEventRequest;
import com.hfing.ticketflowapi.event.entity.Event;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
    @Mapping(target = "bannerUrl", ignore = true)
    @Mapping(target = "shortImageUrl", ignore = true)
    @Mapping(target = "bannerKey", ignore = true)
    @Mapping(target = "shortImageKey", ignore = true)
    Event toEvent(CreateEventRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "shows", ignore = true)
    @Mapping(target = "bannerUrl", ignore = true)
    @Mapping(target = "shortImageUrl", ignore = true)
    @Mapping(target = "bannerKey", ignore = true)
    @Mapping(target = "shortImageKey", ignore = true)
    void updateEventFromRequest(UpdateEventRequest request, @MappingTarget Event event);

    @Mapping(target = "organizerId", source = "organizer.id")
    EventResponse toEventResponse(Event event);

    @Mapping(target = "organizerName", source = "organizerName")
    @Mapping(target = "minPrice", source = "minPrice")
    @Mapping(target = "shows", source = "shows")
    PublicEventResponse toPublicEventResponse(
            Event event,
            String organizerName,
            BigDecimal minPrice,
            List<PublicEventShowResponse> shows);

    @Mapping(target = "organizerName", source = "organizerName")
    @Mapping(target = "minPrice", source = "minPrice")
    @Mapping(target = "day", source = "day")
    PublicEventSummaryResponse toPublicEventSummaryResponse(
            Event event,
            String organizerName,
            BigDecimal minPrice,
            Instant day);
}
