package com.hfing.ticketflowapi.service.impl;

import com.hfing.ticketflowapi.common.enums.RoleType;
import com.hfing.ticketflowapi.dto.request.CreateUserRequest;
import com.hfing.ticketflowapi.dto.response.CreateUserResponse;
import com.hfing.ticketflowapi.dto.response.UserDetailResponse;
import com.hfing.ticketflowapi.entity.Role;
import com.hfing.ticketflowapi.entity.User;
import com.hfing.ticketflowapi.exception.ErrorCode;
import com.hfing.ticketflowapi.exception.AppException;
import com.hfing.ticketflowapi.mapper.UserMapper;
import com.hfing.ticketflowapi.dto.event.UserRegisteredEvent;
import com.hfing.ticketflowapi.repository.UserRepository;
import com.hfing.ticketflowapi.service.RoleService;
import com.hfing.ticketflowapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.extern.slf4j.Slf4j;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = userMapper.toUser(request);

        user.setPassword(passwordEncoder.encode(request.password()));

        Role role = roleService.getRoleByName(RoleType.CUSTOMER.name());

        user.setRole(role);

        User savedUser = userRepository.save(user);

        UserRegisteredEvent event = new UserRegisteredEvent(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName());
        try {
            kafkaTemplate.send("user-registration", savedUser.getEmail(), event);
            log.info("Published user registration event to Kafka for user: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish user registration event to Kafka for user: {}", savedUser.getEmail(), e);
        }

        return userMapper.toCreateUserResponse(savedUser);
    }

    @Override
    public UserDetailResponse myInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public UserDetailResponse updateUser(String userId, com.hfing.ticketflowapi.dto.request.UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userMapper.updateUserFromRequest(request, user);
        User savedUser = userRepository.save(user);
        return userMapper.toUserDetailResponse(savedUser);
    }
}
