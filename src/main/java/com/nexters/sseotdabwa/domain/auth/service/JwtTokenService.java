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
    private static final String TOKEN_TYPE_EMAIL_UNSUBSCRIBE = "email-unsubscribe";

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
     * Access Token 유효성 검증 (서명, 만료, 타입이 access인지 검사)
     * @return 유효한 access 토큰이면 true
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            Claims claims = getClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return TOKEN_TYPE_ACCESS.equals(tokenType);
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

    /**
     * 이메일 알림 수신 거부 링크용 토큰 (type: email-unsubscribe, 만료 없음)
     * - 메일은 오래 지나서 열어볼 수도 있어 만료를 두지 않는다. 할 수 있는 일이 "본인 이메일 알림 끄기"뿐이라 위험이 낮음
     * - type이 access가 아니므로 이 토큰으로 API 인증은 불가 (validateToken에서 거부)
     */
    public String createEmailUnsubscribeToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_EMAIL_UNSUBSCRIBE)
                .issuedAt(new Date())
                .signWith(secretKey)
                .compact();
    }

    /**
     * 이메일 수신 거부 토큰을 검증하고 userId를 반환한다. 서명 위조/형식 오류/다른 타입 토큰이면 null.
     */
    public Long parseEmailUnsubscribeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            Claims claims = getClaims(token);
            if (!TOKEN_TYPE_EMAIL_UNSUBSCRIBE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                return null;
            }
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpiration;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
