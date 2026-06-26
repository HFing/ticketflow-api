package com.hfing.ticketflowapi.mapper;

import com.hfing.ticketflowapi.dto.request.CreateUserRequest;
import com.hfing.ticketflowapi.dto.response.CreateUserResponse;
import com.hfing.ticketflowapi.dto.response.UserDetailResponse;
import com.hfing.ticketflowapi.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    User toUser(CreateUserRequest request);

    CreateUserResponse toCreateUserResponse(User user);
    UserDetailResponse toUserDetailResponse(User user);
}