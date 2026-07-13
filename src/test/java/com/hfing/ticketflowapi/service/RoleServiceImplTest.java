package com.hfing.ticketflowapi.service;

import com.hfing.ticketflowapi.entity.Role;
import com.hfing.ticketflowapi.exception.ErrorCode;
import com.hfing.ticketflowapi.exception.AppException;
import com.hfing.ticketflowapi.repository.RoleRepository;
import com.hfing.ticketflowapi.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void getRoleByName_whenRoleExists_returnsRole() {
        var role = Role.builder()
                .id("role-1")
                .name("ADMIN")
                .description("Administrator")
                .build();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        Role result = roleService.getRoleByName("ADMIN");

        assertThat(result).isSameAs(role);
        assertThat(result.getName()).isEqualTo("ADMIN");
        verify(roleRepository).findByName("ADMIN");
    }

    @Test
    void getRoleByName_whenRoleDoesNotExist_throwsRoleNotFoundException() {
        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleByName("UNKNOWN"))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.ROLE_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROLE_NOT_FOUND);

        verify(roleRepository).findByName("UNKNOWN");
    }
}
