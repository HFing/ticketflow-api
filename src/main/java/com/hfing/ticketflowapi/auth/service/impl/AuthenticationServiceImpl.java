package com.hfing.ticketflowapi.auth.service.impl;

import com.hfing.ticketflowapi.auth.dto.LoginRequest;
import com.hfing.ticketflowapi.auth.dto.LoginResponse;
import com.hfing.ticketflowapi.auth.dto.LoginResult;
import com.hfing.ticketflowapi.auth.dto.TokenDetails;
import com.hfing.ticketflowapi.auth.entity.RedisToken;
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
public class AuthenticationServiceImpl  implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;

    @Override
    public LoginResult login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                );

        Authentication authenticate = authenticationManager.authenticate(authenticationToken);

        User user = (User) authenticate.getPrincipal();
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        String role = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));


        String accessToken = jwtService.generateAccessToken(user.getId(), role);
        TokenDetails refreshToken = jwtService.generateRefreshToken(user.getId());

        RedisToken redisToken = RedisToken.builder()
                .jwtId(refreshToken.jwtId())
                .userId(user.getId())
                .expiration(refreshToken.ttlSeconds())
                .build();

        redisTokenService.saveToken(redisToken);

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
            String userId = signedJWT.getJWTClaimsSet().getSubject();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            String role = user.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

            String newAccessToken = jwtService.generateAccessToken(userId,role);

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .role(role)
                    .build();

        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.TOKEN_INVALID);

        }
    }

    @Override
    public void logout(String refreshToken) throws ParseException, JOSEException {
        // 1. Validate refresh token có tồn tại không
        if (refreshToken == null) {
            throw new AppException(ErrorCode.MISSING_LOGOUT_INFO);
        }

        // 2. Lấy thông tin user từ SecurityContext (từ access token)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null)
            throw new AppException(ErrorCode.TOKEN_INVALID);
        String userId = authentication.getName();

        // 3. Validate refresh token và lấy thông tin
        SignedJWT signedRefreshToken = jwtService.validateToken(refreshToken);

        String refreshUserId = signedRefreshToken.getJWTClaimsSet().getSubject();
        String refreshJwtId = signedRefreshToken.getJWTClaimsSet().getJWTID();

        // 4. Verify userId từ access token và refresh token phải giống nhau
        // Tránh trường hợp user A dùng access token của mình + refresh token của user B
        if (!userId.equals(refreshUserId)) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        // 5. Xóa refresh token khỏi Redis
        // Refresh token đã được lưu vào Redis khi login (Lesson 4.14)
        redisTokenService.deleteTokenByJwtId(refreshJwtId);

        // 6. Lấy thông tin access token từ SecurityContext
        Jwt jwt = (Jwt) authentication.getPrincipal();
        if(jwt == null)
            throw new AppException(ErrorCode.TOKEN_INVALID);

        String accessJwtId = jwt.getId();
        Instant accessExpiration = jwt.getExpiresAt();

        // 7. Tính TTL còn lại của access token
        // TTL = thời gian hết hạn - thời gian hiện tại
        long ttl = ChronoUnit.SECONDS.between(
                Instant.now(),
                accessExpiration
        );

        // 8. Nếu access token còn hạn → lưu vào Redis blacklist
        // Nếu đã hết hạn (ttl <= 0) → không cần lưu vì token đã invalid
        if (ttl > 0) {
            redisTokenService.saveToken(
                    RedisToken.builder()
                            .jwtId(accessJwtId)
                            .userId(userId)
                            .expiration(ttl)
                            .build()
            );
        }
    }


}