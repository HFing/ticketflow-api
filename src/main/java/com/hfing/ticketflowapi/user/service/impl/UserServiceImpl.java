package com.hfing.ticketflowapi.user.service.impl;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.notification.dto.UserRegisteredEvent;
import com.hfing.ticketflowapi.user.dto.CreateUserRequest;
import com.hfing.ticketflowapi.user.dto.CreateUserResponse;
import com.hfing.ticketflowapi.user.dto.UpdateUserRequest;
import com.hfing.ticketflowapi.user.dto.UserDetailResponse;
import com.hfing.ticketflowapi.user.entity.Role;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.enums.RoleType;
import com.hfing.ticketflowapi.user.mapper.UserMapper;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import com.hfing.ticketflowapi.user.service.IRoleService;
import com.hfing.ticketflowapi.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.hfing.ticketflowapi.common.config.KafkaTopicConfiguration.USER_REGISTRATION_TOPIC;


@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final IRoleService roleService;
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
            kafkaTemplate.send(USER_REGISTRATION_TOPIC, savedUser.getEmail(), event);
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
    public UserDetailResponse updateUser(String userId, com.hfing.ticketflowapi.user.dto.UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userMapper.updateUserFromRequest(request, user);
        User savedUser = userRepository.save(user);
        return userMapper.toUserDetailResponse(savedUser);
    }
}
