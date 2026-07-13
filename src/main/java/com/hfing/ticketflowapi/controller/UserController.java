package com.hfing.ticketflowapi.controller;

import com.hfing.ticketflowapi.dto.request.CreateUserRequest;
import com.hfing.ticketflowapi.dto.request.UpdateUserRequest;
import com.hfing.ticketflowapi.dto.response.ApiResponse;
import com.hfing.ticketflowapi.dto.response.CreateUserResponse;
import com.hfing.ticketflowapi.dto.response.UserDetailResponse;
import com.hfing.ticketflowapi.service.UserService;
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
    private final UserService userService;

    @PostMapping
    ApiResponse<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        var data = userService.createUser(request);
        return ApiResponse.<CreateUserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(data)
                .build();
    }

    @GetMapping("/me")
    ApiResponse<UserDetailResponse> getMyInfo (@AuthenticationPrincipal Jwt jwt){
        var userId = jwt.getSubject();
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
        var userId = jwt.getSubject();
        var data = userService.updateUser(userId, request);
        return ApiResponse.<UserDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User info updated successfully")
                .data(data)
                .build();
    }
}
