package com.hfing.ticketflowapi.user.service.impl;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.user.entity.Role;
import com.hfing.ticketflowapi.user.repository.RoleRepository;
import com.hfing.ticketflowapi.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;


    @Override
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }
}