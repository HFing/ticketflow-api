package com.hfing.ticketflowapi.service.impl;

import com.hfing.ticketflowapi.entity.Role;
import com.hfing.ticketflowapi.exception.ErrorCode;
import com.hfing.ticketflowapi.exception.AppException;
import com.hfing.ticketflowapi.repository.RoleRepository;
import com.hfing.ticketflowapi.service.RoleService;
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
