package com.hfing.ticketflowapi.auth.config;

import static com.hfing.ticketflowapi.auth.constant.JWTConstant.TOKEN_TYPE;
import static com.hfing.ticketflowapi.auth.service.IRedisTokenService.ACCESS_TOKEN_BLACKLIST_PREFIX;

import com.hfing.ticketflowapi.auth.enums.TokenType;
import com.hfing.ticketflowapi.auth.service.IRedisTokenService;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

    private final IRedisTokenService redisTokenService;

    @Value("${jwt.secret-key}")
    private String secretKey;

    private NimbusJwtDecoder nimbusJwtDecoder;

    @PostConstruct
    public void init() {
        SecretKey key = new SecretKeySpec(secretKey.getBytes(), "HS512");
        nimbusJwtDecoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = nimbusJwtDecoder.decode(token);
        validateAccessToken(jwt);
        return jwt;
    }

    private void validateAccessToken(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(TOKEN_TYPE);
        if (!TokenType.ACCESS_TOKEN.name().equals(tokenType)) {
            throw new JwtException("Invalid token type");
        }

        if (redisTokenService.existsByJwtId(ACCESS_TOKEN_BLACKLIST_PREFIX + jwt.getId())) {
            throw new JwtException("Access token has been revoked");
        }
    }
}
