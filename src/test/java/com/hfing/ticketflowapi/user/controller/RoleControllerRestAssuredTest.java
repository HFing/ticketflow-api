package com.hfing.ticketflowapi.user.controller;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.common.exception.GlobalExceptionHandler;
import com.hfing.ticketflowapi.user.entity.Role;
import com.hfing.ticketflowapi.user.service.RoleService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;



class RoleControllerRestAssuredTest {

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = mock(RoleService.class);
        RestAssuredMockMvc.standaloneSetup(
                new RoleController(roleService),
                new GlobalExceptionHandler()
        );
    }

    @AfterEach
    void tearDown() {
        RestAssuredMockMvc.reset();
    }

    @Test
    void getRoleByName_whenRoleExists_returnsRole() {
        var role = Role.builder()
                .id("role-1")
                .name("ADMIN")
                .description("Administrator")
                .build();
        when(roleService.getRoleByName("ADMIN")).thenReturn(role);

        RestAssuredMockMvc.given()
                .when()
                .get("/api/v1/roles/{name}", "ADMIN")
                .then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(200))
                .body("message", org.hamcrest.Matchers.equalTo("Role retrieved successfully"))
                .body("data.id", org.hamcrest.Matchers.equalTo("role-1"))
                .body("data.name", org.hamcrest.Matchers.equalTo("ADMIN"))
                .body("data.description", org.hamcrest.Matchers.equalTo("Administrator"));
    }

    @Test
    void getRoleByName_whenRoleDoesNotExist_returnsNotFound() {
        when(roleService.getRoleByName("UNKNOWN"))
                .thenThrow(new AppException(ErrorCode.ROLE_NOT_FOUND));

        RestAssuredMockMvc.given()
                .when()
                .get("/api/v1/roles/{name}", "UNKNOWN")
                .then()
                .statusCode(404)
                .body("code", org.hamcrest.Matchers.equalTo(404))
                .body("error", org.hamcrest.Matchers.equalTo("Not Found"))
                .body("message", org.hamcrest.Matchers.equalTo("Role not found"))
                .body("path", endsWith("/api/v1/roles/UNKNOWN"));
    }
}