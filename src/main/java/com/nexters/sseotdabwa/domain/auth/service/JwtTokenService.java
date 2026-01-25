package com.nexters.sseotdabwa.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 토큰 발급 및 검증 서비스
 * - Access Token: API 인증에 사용 (type: access)
 * - Refresh Token: Access Token 갱신에 사용 (type: refresh)
 */
@Service
public class JwtTokenService {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Access Token 생성 (짧은 만료 시간, type: access)
     */
    public String createAccessToken(Long userId) {
        return createToken(userId, accessTokenExpiration, TOKEN_TYPE_ACCESS);
    }

    /**
     * Refresh Token 생성 (긴 만료 시간, type: refresh)
     */
    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTokenExpiration, TOKEN_TYPE_REFRESH);
    }

    private String createToken(Long userId, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Access Token 유효성 검증 (서명, 만료, 타입 검사)
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Refresh Token 유효성 검증 (서명, 만료, 타입이 refresh인지 검사)
     * @return 유효한 refresh 토큰이면 true
     */
    public boolean validateRefreshToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            Claims claims = getClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return TOKEN_TYPE_REFRESH.equals(tokenType);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
