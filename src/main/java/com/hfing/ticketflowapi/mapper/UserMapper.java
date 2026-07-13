package com.hfing.ticketflowapi.mapper;

import com.hfing.ticketflowapi.dto.request.CreateUserRequest;
import com.hfing.ticketflowapi.dto.request.UpdateUserRequest;
import com.hfing.ticketflowapi.dto.response.CreateUserResponse;
import com.hfing.ticketflowapi.dto.response.UserDetailResponse;
import com.hfing.ticketflowapi.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "events", ignore = true)
    User toUser(CreateUserRequest request);

    CreateUserResponse toCreateUserResponse(User user);
    UserDetailResponse toUserDetailResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "events", ignore = true)
    void updateUserFromRequest(UpdateUserRequest request, @MappingTarget User user);
}