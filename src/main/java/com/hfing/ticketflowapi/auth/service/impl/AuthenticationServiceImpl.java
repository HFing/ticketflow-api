package com.hfing.ticketflowapi.auth.service.impl;

import static com.hfing.ticketflowapi.auth.constant.JWTConstant.TOKEN_TYPE;
import static com.hfing.ticketflowapi.auth.service.RedisTokenService.ACCESS_TOKEN_BLACKLIST_PREFIX;

import com.hfing.ticketflowapi.auth.dto.LoginRequest;
import com.hfing.ticketflowapi.auth.dto.LoginResponse;
import com.hfing.ticketflowapi.auth.dto.LoginResult;
import com.hfing.ticketflowapi.auth.dto.TokenDetails;
import com.hfing.ticketflowapi.auth.entity.RedisToken;
import com.hfing.ticketflowapi.auth.enums.TokenType;
import com.hfing.ticketflowapi.auth.service.AuthenticationService;
import com.hfing.ticketflowapi.auth.service.JwtService;
import com.hfing.ticketflowapi.auth.service.RedisTokenService;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;

    @Override
    public LoginResult login(LoginRequest request) {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = (User) authenticate.getPrincipal();
        String role = extractRole(user);

        String accessToken = jwtService.generateAccessToken(user.getId(), role);
        TokenDetails refreshToken = jwtService.generateRefreshToken(user.getId());

        redisTokenService.saveToken(RedisToken.builder()
                .jwtId(refreshToken.jwtId())
                .userId(user.getId())
                .expiration(refreshToken.ttlSeconds())
                .build());

        return LoginResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.value())
                .role(role)
                .build();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        try {
            SignedJWT signedJWT = jwtService.validateToken(refreshToken);
            validateRefreshToken(signedJWT);

            String refreshJwtId = signedJWT.getJWTClaimsSet().getJWTID();
            if (!redisTokenService.existsByJwtId(refreshJwtId)) {
                throw new AppException(ErrorCode.TOKEN_INVALID);
            }

            String userId = signedJWT.getJWTClaimsSet().getSubject();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            String role = extractRole(user);

            return LoginResponse.builder()
                    .accessToken(jwtService.generateAccessToken(userId, role))
                    .role(role)
                    .build();
        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    @Override
    public void logout(String refreshToken) throws ParseException, JOSEException {
        if (refreshToken == null) {
            throw new AppException(ErrorCode.MISSING_LOGOUT_INFO);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        String userId = authentication.getName();
        SignedJWT signedRefreshToken = jwtService.validateToken(refreshToken);
        validateRefreshToken(signedRefreshToken);

        String refreshUserId = signedRefreshToken.getJWTClaimsSet().getSubject();
        String refreshJwtId = signedRefreshToken.getJWTClaimsSet().getJWTID();
        if (!userId.equals(refreshUserId) || !redisTokenService.existsByJwtId(refreshJwtId)) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        redisTokenService.deleteTokenByJwtId(refreshJwtId);
        blacklistCurrentAccessToken(authentication, userId);
    }

    private String extractRole(User user) {
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        return user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    private void validateRefreshToken(SignedJWT signedJWT) throws ParseException {
        String tokenType = String.valueOf(signedJWT.getJWTClaimsSet().getClaim(TOKEN_TYPE));
        if (!TokenType.REFRESH_TOKEN.name().equals(tokenType)) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    private void blacklistCurrentAccessToken(Authentication authentication, String userId) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Instant accessExpiration = jwt.getExpiresAt();
        if (accessExpiration == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        long ttl = ChronoUnit.SECONDS.between(Instant.now(), accessExpiration);
        if (ttl <= 0) {
            return;
        }

        redisTokenService.saveToken(RedisToken.builder()
                .jwtId(ACCESS_TOKEN_BLACKLIST_PREFIX + jwt.getId())
                .userId(userId)
                .expiration(ttl)
                .build());
    }
}
