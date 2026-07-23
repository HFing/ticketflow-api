package com.hfing.ticketflowapi.user.mapper;

import com.hfing.ticketflowapi.user.dto.CreateUserRequest;
import com.hfing.ticketflowapi.user.dto.CreateUserResponse;
import com.hfing.ticketflowapi.user.dto.UpdateUserRequest;
import com.hfing.ticketflowapi.user.dto.UserDetailResponse;
import com.hfing.ticketflowapi.user.entity.User;
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
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    UserDetailResponse toUserDetailResponse(User user, String avatarUrl);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "avatarKey", ignore = true)
    void updateUserFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
