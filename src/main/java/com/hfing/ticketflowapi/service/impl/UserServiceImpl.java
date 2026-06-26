package com.hfing.ticketflowapi.service.impl;

import com.hfing.ticketflowapi.common.enums.RoleType;
import com.hfing.ticketflowapi.dto.request.CreateUserRequest;
import com.hfing.ticketflowapi.dto.response.CreateUserResponse;
import com.hfing.ticketflowapi.dto.response.UserDetailResponse;
import com.hfing.ticketflowapi.entity.Role;
import com.hfing.ticketflowapi.entity.User;
import com.hfing.ticketflowapi.exception.ErrorCode;
import com.hfing.ticketflowapi.exception.UserServiceException;
import com.hfing.ticketflowapi.mapper.UserMapper;
import com.hfing.ticketflowapi.repository.UserRepository;
import com.hfing.ticketflowapi.service.RoleService;
import com.hfing.ticketflowapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RoleService roleService;

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException(ErrorCode.USER_ALREADY_EXISTS.getMessage());
        }

        User user = userMapper.toUser(request);

        user.setPassword(passwordEncoder.encode(request.password()));

        Role role = roleService.getRoleByName(RoleType.CUSTOMER.name());

        user.setRole(role);

        User savedUser = userRepository.save(user);

        return userMapper.toCreateUserResponse(savedUser);
    }

    @Override
    public UserDetailResponse myInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserDetailResponse(user);
    }
}
