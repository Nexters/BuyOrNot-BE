package com.nexters.sseotdabwa.domain.auth.service;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.service.external.ApplePublicKeys;
import com.nexters.sseotdabwa.domain.auth.service.external.AppleTokenResponse;
import com.nexters.sseotdabwa.domain.auth.service.external.AppleUserInfo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Apple OAuth 서비스
 * - Authorization Code를 Apple Token API로 교환
 * - Identity Token(JWT) 검증 및 사용자 정보 추출
 */
@Service
@RequiredArgsConstructor
public class AppleOAuthService {

    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);
    private static final long CLIENT_SECRET_EXPIRATION = 1000 * 60 * 5; // 5분

    private final WebClient webClient;

    @Value("${oauth.apple.keys-url}")
    private String keysUrl;

    @Value("${oauth.apple.token-url}")
    private String tokenUrl;

    @Value("${oauth.apple.issuer}")
    private String issuer;

    @Value("${oauth.apple.team-id}")
    private String teamId;

    @Value("${oauth.apple.key-id}")
    private String keyId;

    @Value("${oauth.apple.client-id}")
    private String clientId;

    @Value("${oauth.apple.private-key}")
    private String privateKey;

    /**
     * Authorization Code로 Apple 사용자 정보 조회
     * 1. client_secret JWT 생성
     * 2. Apple Token API 호출하여 id_token 획득
     * 3. id_token 검증 및 사용자 정보 추출
     *
     * @param authorizationCode Apple 로그인 후 받은 Authorization Code
     * @return Apple 사용자 정보 (sub)
     */
    public AppleUserInfo getAppleUserInfo(String authorizationCode) {
        // 1. client_secret JWT 생성
        String clientSecret = generateClientSecret();

        // 2. Apple Token API 호출
        AppleTokenResponse tokenResponse = exchangeCodeForToken(authorizationCode, clientSecret);

        // 3. id_token 검증 및 사용자 정보 추출
        return verifyAndGetUserInfo(tokenResponse.getIdToken());
    }

    /**
     * client_secret JWT 생성
     * Apple Token API 호출 시 인증용
     */
    private String generateClientSecret() {
        try {
            Date now = new Date();
            Date expiration = new Date(now.getTime() + CLIENT_SECRET_EXPIRATION);

            PrivateKey key = getPrivateKey();

            return Jwts.builder()
                    .header()
                    .keyId(keyId)
                    .and()
                    .issuer(teamId)
                    .issuedAt(now)
                    .expiration(expiration)
                    .audience().add("https://appleid.apple.com").and()
                    .subject(clientId)
                    .signWith(key)
                    .compact();
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.APPLE_CLIENT_SECRET_GENERATION_FAILED);
        }
    }

    /**
     * .p8 Private Key 파싱
     */
    private PrivateKey getPrivateKey() {
        try {
            String privateKeyContent = privateKey
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.APPLE_INVALID_PRIVATE_KEY);
        }
    }

    /**
     * Authorization Code를 Apple Token API로 교환
     */
    private AppleTokenResponse exchangeCodeForToken(String authorizationCode, String clientSecret) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("code", authorizationCode);
        formData.add("grant_type", "authorization_code");

        try {
            return webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            Mono.error(new GlobalException(AuthErrorCode.APPLE_INVALID_AUTHORIZATION_CODE)))
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new GlobalException(AuthErrorCode.APPLE_TOKEN_API_FAILED)))
                    .bodyToMono(AppleTokenResponse.class)
                    .timeout(API_TIMEOUT)
                    .onErrorMap(TimeoutException.class, e ->
                            new GlobalException(AuthErrorCode.APPLE_API_TIMEOUT))
                    .blockOptional()
                    .orElseThrow(() -> new GlobalException(AuthErrorCode.APPLE_TOKEN_API_FAILED));
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.APPLE_TOKEN_API_FAILED);
        }
    }

    /**
     * Identity Token 검증 및 사용자 정보 추출
     * @param identityToken Apple Token API에서 받은 id_token (JWT)
     * @return Apple 사용자 정보 (sub)
     */
    public AppleUserInfo verifyAndGetUserInfo(String identityToken) {
        // 1. Identity Token 헤더에서 kid 추출
        String kid = extractKidFromToken(identityToken);

        // 2. Apple 공개키 조회
        ApplePublicKeys applePublicKeys = fetchApplePublicKeys();

        // 3. kid와 일치하는 공개키 찾기
        ApplePublicKeys.Key matchedKey = applePublicKeys.getMatchedKey(kid);
        if (matchedKey == null) {
            throw new GlobalException(AuthErrorCode.APPLE_INVALID_TOKEN);
        }

        // 4. 공개키로 JWT 검증 및 Claims 추출
        PublicKey publicKey = generatePublicKey(matchedKey);
        Claims claims = verifyAndGetClaims(identityToken, publicKey);

        // 5. Claims 검증 (issuer, audience)
        validateClaims(claims);

        // 6. 사용자 정보 추출
        return extractUserInfo(claims);
    }

    private String extractKidFromToken(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            if (parts.length != 3) {
                throw new GlobalException(AuthErrorCode.APPLE_INVALID_TOKEN);
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            // 간단한 JSON 파싱 (kid 추출)
            int kidIndex = headerJson.indexOf("\"kid\"");
            if (kidIndex == -1) {
                throw new GlobalException(AuthErrorCode.APPLE_INVALID_TOKEN);
            }

            int colonIndex = headerJson.indexOf(":", kidIndex);
            int quoteStart = headerJson.indexOf("\"", colonIndex);
            int quoteEnd = headerJson.indexOf("\"", quoteStart + 1);
            return headerJson.substring(quoteStart + 1, quoteEnd);
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.APPLE_INVALID_TOKEN);
        }
    }

    private ApplePublicKeys fetchApplePublicKeys() {
        try {
            return webClient.get()
                    .uri(keysUrl)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            Mono.error(new GlobalException(AuthErrorCode.APPLE_KEY_FETCH_FAILED)))
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new GlobalException(AuthErrorCode.APPLE_KEY_FETCH_FAILED)))
                    .bodyToMono(ApplePublicKeys.class)
                    .timeout(API_TIMEOUT)
                    .onErrorMap(TimeoutException.class, e ->
                            new GlobalException(AuthErrorCode.APPLE_API_TIMEOUT))
                    .blockOptional()
                    .orElseThrow(() -> new GlobalException(AuthErrorCode.APPLE_KEY_FETCH_FAILED));
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.APPLE_KEY_FETCH_FAILED);
        }
    }

    private PublicKey generatePublicKey(ApplePublicKeys.Key key) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(key.getN());
            byte[] eBytes = Base64.getUrlDecoder().decode(key.getE());

            BigInteger n = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger(1, eBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.APPLE_INVALID_TOKEN);
        }
    }

    private Claims verifyAndGetClaims(String identityToken, PublicKey publicKey) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new GlobalException(AuthErrorCode.APPLE_TOKEN_EXPIRED);
        } catch (SignatureException e) {
            throw new GlobalException(AuthErrorCode.APPLE_INVALID_TOKEN);
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.APPLE_INVALID_TOKEN);
        }
    }

    private void validateClaims(Claims claims) {
        // issuer 검증
        String tokenIssuer = claims.getIssuer();
        if (!issuer.equals(tokenIssuer)) {
            throw new GlobalException(AuthErrorCode.APPLE_INVALID_ISSUER);
        }

        // audience 검증
        if (claims.getAudience() == null || !claims.getAudience().contains(clientId)) {
            throw new GlobalException(AuthErrorCode.APPLE_INVALID_AUDIENCE);
        }
    }

    private AppleUserInfo extractUserInfo(Claims claims) {
        return AppleUserInfo.builder()
                .sub(claims.getSubject())
                .build();
    }
}
