package com.nexters.sseotdabwa.domain.auth.service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.service.external.ApplePublicKeys;
import com.nexters.sseotdabwa.domain.auth.service.external.AppleUserInfo;

import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppleOAuthServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private AppleOAuthService appleOAuthService;

    private KeyPair testKeyPair;
    private static final String TEST_KID = "test-kid-123";
    private static final String TEST_CLIENT_ID = "com.sseotdabwa.buyornot.login";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        testKeyPair = keyPairGenerator.generateKeyPair();

        ReflectionTestUtils.setField(appleOAuthService, "keysUrl", "https://appleid.apple.com/auth/keys");
        ReflectionTestUtils.setField(appleOAuthService, "tokenUrl", "https://appleid.apple.com/auth/token");
        ReflectionTestUtils.setField(appleOAuthService, "issuer", APPLE_ISSUER);
        ReflectionTestUtils.setField(appleOAuthService, "teamId", "TEAM123456");
        ReflectionTestUtils.setField(appleOAuthService, "keyId", "KEY123456");
        ReflectionTestUtils.setField(appleOAuthService, "clientId", TEST_CLIENT_ID);
        // EC Private Key는 테스트에서 모킹으로 우회
    }

    @Test
    @DisplayName("유효한 Identity Token으로 사용자 정보 조회 성공")
    void verifyAndGetUserInfo_success() {
        // given
        String identityToken = createValidIdentityToken("test-sub-123", "test@icloud.com");
        ApplePublicKeys mockKeys = createMockApplePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when
        AppleUserInfo result = appleOAuthService.verifyAndGetUserInfo(identityToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSub()).isEqualTo("test-sub-123");
        assertThat(result.getEmail()).isEqualTo("test@icloud.com");
    }

    @Test
    @DisplayName("email 없는 Identity Token으로도 사용자 정보 조회 성공")
    void verifyAndGetUserInfo_withoutEmail_success() {
        // given
        String identityToken = createValidIdentityToken("test-sub-456", null);
        ApplePublicKeys mockKeys = createMockApplePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when
        AppleUserInfo result = appleOAuthService.verifyAndGetUserInfo(identityToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSub()).isEqualTo("test-sub-456");
        assertThat(result.getEmail()).isNull();
    }

    @Test
    @DisplayName("만료된 토큰으로 조회 시 AUTH_202 예외 발생")
    void verifyAndGetUserInfo_expiredToken_throwsException() {
        // given
        String expiredToken = createExpiredIdentityToken("test-sub-123");
        ApplePublicKeys mockKeys = createMockApplePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when & then
        assertThatThrownBy(() -> appleOAuthService.verifyAndGetUserInfo(expiredToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.APPLE_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("잘못된 서명의 토큰으로 조회 시 AUTH_201 예외 발생")
    void verifyAndGetUserInfo_invalidSignature_throwsException() throws Exception {
        // given
        String invalidSignatureToken = createTokenWithDifferentKey("test-sub-123");
        ApplePublicKeys mockKeys = createMockApplePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when & then
        assertThatThrownBy(() -> appleOAuthService.verifyAndGetUserInfo(invalidSignatureToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.APPLE_INVALID_TOKEN);
    }

    @Test
    @DisplayName("잘못된 issuer 토큰으로 조회 시 AUTH_203 예외 발생")
    void verifyAndGetUserInfo_invalidIssuer_throwsException() {
        // given
        String invalidIssuerToken = createTokenWithInvalidIssuer("test-sub-123");
        ApplePublicKeys mockKeys = createMockApplePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when & then
        assertThatThrownBy(() -> appleOAuthService.verifyAndGetUserInfo(invalidIssuerToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.APPLE_INVALID_ISSUER);
    }

    @Test
    @DisplayName("잘못된 audience 토큰으로 조회 시 AUTH_204 예외 발생")
    void verifyAndGetUserInfo_invalidAudience_throwsException() {
        // given
        String invalidAudienceToken = createTokenWithInvalidAudience("test-sub-123");
        ApplePublicKeys mockKeys = createMockApplePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when & then
        assertThatThrownBy(() -> appleOAuthService.verifyAndGetUserInfo(invalidAudienceToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.APPLE_INVALID_AUDIENCE);
    }

    @Test
    @DisplayName("Apple 공개키 조회 실패 시 AUTH_205 예외 발생")
    void verifyAndGetUserInfo_keyFetchFailed_throwsException() {
        // given
        String identityToken = createValidIdentityToken("test-sub-123");

        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(ApplePublicKeys.class))
                .willReturn(Mono.error(new RuntimeException("Connection failed")));

        // when & then
        assertThatThrownBy(() -> appleOAuthService.verifyAndGetUserInfo(identityToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.APPLE_KEY_FETCH_FAILED);
    }

    @Test
    @DisplayName("매칭되는 공개키가 없으면 AUTH_201 예외 발생")
    void verifyAndGetUserInfo_noMatchingKey_throwsException() {
        // given
        String identityToken = createValidIdentityToken("test-sub-123");
        ApplePublicKeys emptyKeys = new ApplePublicKeys();
        ReflectionTestUtils.setField(emptyKeys, "keys", List.of());

        setupWebClientGetMock(emptyKeys);

        // when & then
        assertThatThrownBy(() -> appleOAuthService.verifyAndGetUserInfo(identityToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.APPLE_INVALID_TOKEN);
    }

    private String createValidIdentityToken(String sub) {
        return createValidIdentityToken(sub, null);
    }

    private String createValidIdentityToken(String sub, String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 3600000); // 1시간 후

        var builder = Jwts.builder()
                .header()
                .keyId(TEST_KID)
                .and()
                .issuer(APPLE_ISSUER)
                .audience().add(TEST_CLIENT_ID).and()
                .subject(sub)
                .issuedAt(now)
                .expiration(expiration);

        if (email != null) {
            builder.claim("email", email);
        }

        return builder.signWith(testKeyPair.getPrivate())
                .compact();
    }

    private String createExpiredIdentityToken(String sub) {
        Date past = new Date(System.currentTimeMillis() - 7200000); // 2시간 전
        Date expiration = new Date(System.currentTimeMillis() - 3600000); // 1시간 전 만료

        return Jwts.builder()
                .header()
                .keyId(TEST_KID)
                .and()
                .issuer(APPLE_ISSUER)
                .audience().add(TEST_CLIENT_ID).and()
                .subject(sub)
                .issuedAt(past)
                .expiration(expiration)
                .signWith(testKeyPair.getPrivate())
                .compact();
    }

    private String createTokenWithDifferentKey(String sub) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair differentKeyPair = keyPairGenerator.generateKeyPair();

        Date now = new Date();
        Date expiration = new Date(now.getTime() + 3600000);

        return Jwts.builder()
                .header()
                .keyId(TEST_KID)
                .and()
                .issuer(APPLE_ISSUER)
                .audience().add(TEST_CLIENT_ID).and()
                .subject(sub)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(differentKeyPair.getPrivate())
                .compact();
    }

    private String createTokenWithInvalidIssuer(String sub) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 3600000);

        return Jwts.builder()
                .header()
                .keyId(TEST_KID)
                .and()
                .issuer("https://invalid-issuer.com")
                .audience().add(TEST_CLIENT_ID).and()
                .subject(sub)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(testKeyPair.getPrivate())
                .compact();
    }

    private String createTokenWithInvalidAudience(String sub) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 3600000);

        return Jwts.builder()
                .header()
                .keyId(TEST_KID)
                .and()
                .issuer(APPLE_ISSUER)
                .audience().add("invalid.bundle.id").and()
                .subject(sub)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(testKeyPair.getPrivate())
                .compact();
    }

    private ApplePublicKeys createMockApplePublicKeys() {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) testKeyPair.getPublic();

        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getPublicExponent().toByteArray());

        ApplePublicKeys.Key key = new ApplePublicKeys.Key();
        ReflectionTestUtils.setField(key, "kty", "RSA");
        ReflectionTestUtils.setField(key, "kid", TEST_KID);
        ReflectionTestUtils.setField(key, "use", "sig");
        ReflectionTestUtils.setField(key, "alg", "RS256");
        ReflectionTestUtils.setField(key, "n", n);
        ReflectionTestUtils.setField(key, "e", e);

        ApplePublicKeys keys = new ApplePublicKeys();
        ReflectionTestUtils.setField(keys, "keys", List.of(key));

        return keys;
    }

    private void setupWebClientGetMock(ApplePublicKeys mockKeys) {
        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(ApplePublicKeys.class)).willReturn(Mono.just(mockKeys));
    }
}
