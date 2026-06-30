package com.hfing.ticketflowapi.controller;

import com.hfing.ticketflowapi.entity.Role;
import com.hfing.ticketflowapi.exception.ErrorCode;
import com.hfing.ticketflowapi.exception.GlobalExceptionHandler;
import com.hfing.ticketflowapi.exception.UserServiceException;
import com.hfing.ticketflowapi.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleControllerMockMvcTest {

    private MockMvc mockMvc;
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = mock(RoleService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoleController(roleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getRoleByName_whenRoleExists_returnsRole() throws Exception {
        var role = Role.builder()
                .id("role-1")
                .name("ADMIN")
                .description("Administrator")
                .build();
        when(roleService.getRoleByName("ADMIN")).thenReturn(role);

        mockMvc.perform(get("/api/v1/roles/{name}", "ADMIN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Role retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value("role-1"))
                .andExpect(jsonPath("$.data.name").value("ADMIN"))
                .andExpect(jsonPath("$.data.description").value("Administrator"));
    }

    @Test
    void getRoleByName_whenRoleDoesNotExist_returnsNotFound() throws Exception {
        when(roleService.getRoleByName("UNKNOWN"))
                .thenThrow(new UserServiceException(ErrorCode.ROLE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/roles/{name}", "UNKNOWN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Role not found"))
                .andExpect(jsonPath("$.path", endsWith("/api/v1/roles/UNKNOWN")));
    }
}
