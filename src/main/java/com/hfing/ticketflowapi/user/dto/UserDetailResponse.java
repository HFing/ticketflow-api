package com.hfing.ticketflowapi.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hfing.ticketflowapi.user.enums.UserStatus;
import lombok.Builder;


@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDetailResponse(
        String email,
        String firstName,
        String lastName,
        String phone,
        String avatarKey,
        UserStatus userStatus,
        String coverKey,
        String description
) {}