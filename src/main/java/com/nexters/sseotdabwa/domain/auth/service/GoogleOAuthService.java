package com.nexters.sseotdabwa.domain.auth.service;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.service.external.GooglePublicKeys;
import com.nexters.sseotdabwa.domain.auth.service.external.GoogleUserInfo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Google OAuth 서비스
 * - ID Token(JWT) 검증 및 사용자 정보 추출
 * - iOS, Android, Web 모두 동일한 방식으로 ID Token 검증
 */
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);
    private static final Set<String> VALID_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    private final WebClient webClient;

    @Value("${oauth.google.keys-url}")
    private String keysUrl;

    @Value("${oauth.google.issuer}")
    private String issuer;

    @Value("${oauth.google.web-client-id}")
    private String webClientId;

    @Value("${oauth.google.ios-client-id}")
    private String iosClientId;

    @Value("${oauth.google.android-client-id}")
    private String androidClientId;

    /**
     * ID Token 검증 및 사용자 정보 추출
     * @param idToken Google SDK에서 받은 ID Token (JWT)
     * @return Google 사용자 정보 (sub)
     */
    public GoogleUserInfo verifyAndGetUserInfo(String idToken) {
        // 1. ID Token 헤더에서 kid 추출
        String kid = extractKidFromToken(idToken);

        // 2. Google 공개키 조회
        GooglePublicKeys googlePublicKeys = fetchGooglePublicKeys();

        // 3. kid와 일치하는 공개키 찾기
        GooglePublicKeys.Key matchedKey = googlePublicKeys.getMatchedKey(kid);
        if (matchedKey == null) {
            throw new GlobalException(AuthErrorCode.GOOGLE_INVALID_TOKEN);
        }

        // 4. 공개키로 JWT 검증 및 Claims 추출
        PublicKey publicKey = generatePublicKey(matchedKey);
        Claims claims = verifyAndGetClaims(idToken, publicKey);

        // 5. Claims 검증 (issuer, audience)
        validateClaims(claims);

        // 6. 사용자 정보 추출
        return extractUserInfo(claims);
    }

    private String extractKidFromToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new GlobalException(AuthErrorCode.GOOGLE_INVALID_TOKEN);
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            // 간단한 JSON 파싱 (kid 추출)
            int kidIndex = headerJson.indexOf("\"kid\"");
            if (kidIndex == -1) {
                throw new GlobalException(AuthErrorCode.GOOGLE_INVALID_TOKEN);
            }

            int colonIndex = headerJson.indexOf(":", kidIndex);
            int quoteStart = headerJson.indexOf("\"", colonIndex);
            int quoteEnd = headerJson.indexOf("\"", quoteStart + 1);
            return headerJson.substring(quoteStart + 1, quoteEnd);
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.GOOGLE_INVALID_TOKEN);
        }
    }

    private GooglePublicKeys fetchGooglePublicKeys() {
        try {
            return webClient.get()
                    .uri(keysUrl)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            Mono.error(new GlobalException(AuthErrorCode.GOOGLE_KEY_FETCH_FAILED)))
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new GlobalException(AuthErrorCode.GOOGLE_KEY_FETCH_FAILED)))
                    .bodyToMono(GooglePublicKeys.class)
                    .timeout(API_TIMEOUT)
                    .onErrorMap(TimeoutException.class, e ->
                            new GlobalException(AuthErrorCode.GOOGLE_API_TIMEOUT))
                    .blockOptional()
                    .orElseThrow(() -> new GlobalException(AuthErrorCode.GOOGLE_KEY_FETCH_FAILED));
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.GOOGLE_KEY_FETCH_FAILED);
        }
    }

    private PublicKey generatePublicKey(GooglePublicKeys.Key key) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(key.getN());
            byte[] eBytes = Base64.getUrlDecoder().decode(key.getE());

            BigInteger n = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger(1, eBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.GOOGLE_INVALID_TOKEN);
        }
    }

    private Claims verifyAndGetClaims(String idToken, PublicKey publicKey) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new GlobalException(AuthErrorCode.GOOGLE_TOKEN_EXPIRED);
        } catch (SignatureException e) {
            throw new GlobalException(AuthErrorCode.GOOGLE_INVALID_TOKEN);
        } catch (Exception e) {
            throw new GlobalException(AuthErrorCode.GOOGLE_INVALID_TOKEN);
        }
    }

    private void validateClaims(Claims claims) {
        // issuer 검증 (Google은 두 가지 issuer를 사용)
        String tokenIssuer = claims.getIssuer();
        if (!VALID_ISSUERS.contains(tokenIssuer)) {
            throw new GlobalException(AuthErrorCode.GOOGLE_INVALID_ISSUER);
        }

        // audience 검증 (Web, iOS, Android 클라이언트 ID 중 하나와 일치해야 함)
        Set<String> validClientIds = Set.of(webClientId, iosClientId, androidClientId);
        if (claims.getAudience() == null ||
                claims.getAudience().stream().noneMatch(validClientIds::contains)) {
            throw new GlobalException(AuthErrorCode.GOOGLE_INVALID_AUDIENCE);
        }
    }

    private GoogleUserInfo extractUserInfo(Claims claims) {
        return GoogleUserInfo.builder()
                .sub(claims.getSubject())
                .email(claims.get("email", String.class))
                .build();
    }
}
