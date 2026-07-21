package com.hfing.ticketflowapi.user.service;

import com.hfing.ticketflowapi.user.dto.CreateUserRequest;
import com.hfing.ticketflowapi.user.dto.CreateUserResponse;
import com.hfing.ticketflowapi.user.dto.UpdateUserRequest;
import com.hfing.ticketflowapi.user.dto.UserDetailResponse;

public interface IUserService {
    CreateUserResponse createUser(CreateUserRequest request);

    UserDetailResponse myInfo(String userId);

    UserDetailResponse updateUser(String userId, UpdateUserRequest request);
}
