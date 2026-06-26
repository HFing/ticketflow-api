package com.hfing.ticketflowapi.service.impl;

import com.hfing.ticketflowapi.dto.request.LoginRequest;
import com.hfing.ticketflowapi.dto.response.LoginResponse;
import com.hfing.ticketflowapi.entity.User;
import com.hfing.ticketflowapi.exception.ErrorCode;
import com.hfing.ticketflowapi.exception.UserServiceException;
import com.hfing.ticketflowapi.repository.UserRepository;
import com.hfing.ticketflowapi.service.AuthenticationService;
import com.hfing.ticketflowapi.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl  implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                );

        Authentication authenticate = authenticationManager.authenticate(authenticationToken);

        User user = (User) authenticate.getPrincipal();
        if (user == null) {
            throw new UserServiceException(ErrorCode.USER_NOT_FOUND);
        }

        String role = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new UserServiceException(ErrorCode.ROLE_NOT_FOUND));


        String accessToken = jwtService.generateAccessToken(user.getId(), role);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .role(role)
                .build();
    }
}
