package com.hfing.ticketflowapi.auth.service.impl;

import static com.hfing.ticketflowapi.auth.constant.JWTConstant.JWT_ISSUER;
import static com.hfing.ticketflowapi.auth.constant.JWTConstant.ROLES;
import static com.hfing.ticketflowapi.auth.constant.JWTConstant.TOKEN_TYPE;

import com.hfing.ticketflowapi.auth.dto.TokenDetails;
import com.hfing.ticketflowapi.auth.enums.TokenType;
import com.hfing.ticketflowapi.auth.service.JwtService;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Override
    public String generateAccessToken(String userId, String role) {
        Date issueTime = new Date();
        Date expiredTime = new Date(Instant.now().plus(2, ChronoUnit.HOURS).toEpochMilli());

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issuer(JWT_ISSUER)
                .claim(ROLES, role)
                .issueTime(issueTime)
                .expirationTime(expiredTime)
                .jwtID(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE, TokenType.ACCESS_TOKEN.name())
                .build();

        return sign(claimsSet);
    }

    @Override
    public TokenDetails generateRefreshToken(String userId) {
        Date issueTime = new Date();
        Date expiredTime = new Date(Instant.now().plus(14, ChronoUnit.DAYS).toEpochMilli());
        long ttlSeconds = ChronoUnit.SECONDS.between(Instant.now(), expiredTime.toInstant());
        String jwtId = UUID.randomUUID().toString();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issuer(JWT_ISSUER)
                .issueTime(issueTime)
                .expirationTime(expiredTime)
                .jwtID(jwtId)
                .claim(TOKEN_TYPE, TokenType.REFRESH_TOKEN.name())
                .build();

        return TokenDetails.builder()
                .value(sign(claimsSet))
                .jwtId(jwtId)
                .ttlSeconds(ttlSeconds)
                .build();
    }

    @Override
    public SignedJWT validateToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
        if (expiration == null || expiration.before(new Date())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        boolean verified = signedJWT.verify(new MACVerifier(secretKey));
        if (!verified) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        return signedJWT;
    }

    private String sign(JWTClaimsSet claimsSet) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWSObject jwsObject = new JWSObject(header, new Payload(claimsSet.toJSONObject()));

        try {
            jwsObject.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            throw new AppException(ErrorCode.TOKEN_GENERATION_FAILED);
        }

        return jwsObject.serialize();
    }
}
