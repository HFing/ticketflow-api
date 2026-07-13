package com.hfing.ticketflowapi.mapper;

import com.hfing.ticketflowapi.dto.request.EventShowRequest;
import com.hfing.ticketflowapi.dto.response.EventShowResponse;
import com.hfing.ticketflowapi.entity.EventShow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventShowMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    EventShow toEventShow(EventShowRequest request);

    EventShowResponse toEventShowResponse(EventShow eventShow);
}
