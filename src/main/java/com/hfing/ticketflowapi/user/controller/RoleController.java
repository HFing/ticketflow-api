package com.hfing.ticketflowapi.user.controller;

import com.hfing.ticketflowapi.common.response.ApiResponse;
import com.hfing.ticketflowapi.user.entity.Role;
import com.hfing.ticketflowapi.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/{name}")
    ApiResponse<Role> getRoleByName(@PathVariable String name) {
        var data = roleService.getRoleByName(name);
        return ApiResponse.<Role>builder()
                .code(HttpStatus.OK.value())
                .message("Role retrieved successfully")
                .data(data)
                .build();
    }
}