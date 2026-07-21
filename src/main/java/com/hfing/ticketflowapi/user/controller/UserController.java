package com.hfing.ticketflowapi.user.controller;

import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.common.validation.ControllerInputValidator;
import com.hfing.ticketflowapi.user.dto.CreateUserRequest;
import com.hfing.ticketflowapi.user.dto.CreateUserResponse;
import com.hfing.ticketflowapi.user.dto.UpdateUserRequest;
import com.hfing.ticketflowapi.user.dto.UserDetailResponse;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final IUserService userService;

    @PostMapping
    ApiResponse<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        var validatedRequest = ControllerInputValidator.requireRequestBody(request);
        var data = userService.createUser(validatedRequest);
        return ApiResponse.<CreateUserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(data)
                .build();
    }

    @GetMapping("/me")
    ApiResponse<UserDetailResponse> getMyInfo (@AuthenticationPrincipal Jwt jwt){
        var userId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        var data = userService.myInfo(userId);
        return ApiResponse.<UserDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User info retrieved successfully")
                .data(data)
                .build();
    }

    @PutMapping("/me")
    ApiResponse<UserDetailResponse> updateMyInfo(
            @RequestBody @Valid UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var userId = ControllerInputValidator.requireAuthenticatedSubject(jwt);
        var validatedRequest = ControllerInputValidator.requireRequestBody(request);
        var data = userService.updateUser(userId, validatedRequest);
        return ApiResponse.<UserDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User info updated successfully")
                .data(data)
                .build();
    }
}
