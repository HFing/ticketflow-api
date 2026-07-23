package com.hfing.ticketflowapi.user.service;

import com.hfing.ticketflowapi.user.dto.CreateUserRequest;
import com.hfing.ticketflowapi.user.dto.CreateUserResponse;
import com.hfing.ticketflowapi.user.dto.UpdateUserRequest;
import com.hfing.ticketflowapi.user.dto.UserDetailResponse;
import com.hfing.ticketflowapi.mediaupload.dto.response.FileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IUserService {
    CreateUserResponse createUser(CreateUserRequest request);

    UserDetailResponse myInfo(String userId);

    UserDetailResponse updateUser(String userId, UpdateUserRequest request);

    FileResponse updateAvatar(String userId, MultipartFile file);
}
