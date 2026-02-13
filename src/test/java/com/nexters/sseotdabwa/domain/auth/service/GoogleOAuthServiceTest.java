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
import com.nexters.sseotdabwa.domain.auth.service.external.GooglePublicKeys;
import com.nexters.sseotdabwa.domain.auth.service.external.GoogleUserInfo;

import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    private KeyPair testKeyPair;
    private static final String TEST_KID = "test-kid-123";
    private static final String TEST_WEB_CLIENT_ID = "364719359261-17n2pv8jajf3ub8t0fn6q69k2cu1u1i4.apps.googleusercontent.com";
    private static final String TEST_IOS_CLIENT_ID = "364719359261-h7jnup9nm60clt3v90qrg8h1n256u290.apps.googleusercontent.com";
    private static final String TEST_ANDROID_CLIENT_ID = "test-android-client-id";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        testKeyPair = keyPairGenerator.generateKeyPair();

        ReflectionTestUtils.setField(googleOAuthService, "keysUrl", "https://www.googleapis.com/oauth2/v3/certs");
        ReflectionTestUtils.setField(googleOAuthService, "issuer", GOOGLE_ISSUER);
        ReflectionTestUtils.setField(googleOAuthService, "webClientId", TEST_WEB_CLIENT_ID);
        ReflectionTestUtils.setField(googleOAuthService, "iosClientId", TEST_IOS_CLIENT_ID);
        ReflectionTestUtils.setField(googleOAuthService, "androidClientId", TEST_ANDROID_CLIENT_ID);
    }

    @Test
    @DisplayName("유효한 ID Token(Web Client)으로 사용자 정보 조회 성공")
    void verifyAndGetUserInfo_webClient_success() {
        // given
        String idToken = createValidIdToken("test-sub-123", TEST_WEB_CLIENT_ID, "test@gmail.com");
        GooglePublicKeys mockKeys = createMockGooglePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when
        GoogleUserInfo result = googleOAuthService.verifyAndGetUserInfo(idToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSub()).isEqualTo("test-sub-123");
        assertThat(result.getEmail()).isEqualTo("test@gmail.com");
    }

    @Test
    @DisplayName("유효한 ID Token(iOS Client)으로 사용자 정보 조회 성공")
    void verifyAndGetUserInfo_iosClient_success() {
        // given
        String idToken = createValidIdToken("test-sub-456", TEST_IOS_CLIENT_ID, "ios@gmail.com");
        GooglePublicKeys mockKeys = createMockGooglePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when
        GoogleUserInfo result = googleOAuthService.verifyAndGetUserInfo(idToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSub()).isEqualTo("test-sub-456");
        assertThat(result.getEmail()).isEqualTo("ios@gmail.com");
    }

    @Test
    @DisplayName("만료된 토큰으로 조회 시 AUTH_302 예외 발생")
    void verifyAndGetUserInfo_expiredToken_throwsException() {
        // given
        String expiredToken = createExpiredIdToken("test-sub-123");
        GooglePublicKeys mockKeys = createMockGooglePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when & then
        assertThatThrownBy(() -> googleOAuthService.verifyAndGetUserInfo(expiredToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.GOOGLE_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("잘못된 서명의 토큰으로 조회 시 AUTH_301 예외 발생")
    void verifyAndGetUserInfo_invalidSignature_throwsException() throws Exception {
        // given
        String invalidSignatureToken = createTokenWithDifferentKey("test-sub-123");
        GooglePublicKeys mockKeys = createMockGooglePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when & then
        assertThatThrownBy(() -> googleOAuthService.verifyAndGetUserInfo(invalidSignatureToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.GOOGLE_INVALID_TOKEN);
    }

    @Test
    @DisplayName("잘못된 issuer 토큰으로 조회 시 AUTH_303 예외 발생")
    void verifyAndGetUserInfo_invalidIssuer_throwsException() {
        // given
        String invalidIssuerToken = createTokenWithInvalidIssuer("test-sub-123");
        GooglePublicKeys mockKeys = createMockGooglePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when & then
        assertThatThrownBy(() -> googleOAuthService.verifyAndGetUserInfo(invalidIssuerToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.GOOGLE_INVALID_ISSUER);
    }

    @Test
    @DisplayName("잘못된 audience 토큰으로 조회 시 AUTH_304 예외 발생")
    void verifyAndGetUserInfo_invalidAudience_throwsException() {
        // given
        String invalidAudienceToken = createTokenWithInvalidAudience("test-sub-123");
        GooglePublicKeys mockKeys = createMockGooglePublicKeys();

        setupWebClientGetMock(mockKeys);

        // when & then
        assertThatThrownBy(() -> googleOAuthService.verifyAndGetUserInfo(invalidAudienceToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.GOOGLE_INVALID_AUDIENCE);
    }

    @Test
    @DisplayName("Google 공개키 조회 실패 시 AUTH_305 예외 발생")
    void verifyAndGetUserInfo_keyFetchFailed_throwsException() {
        // given
        String idToken = createValidIdToken("test-sub-123", TEST_WEB_CLIENT_ID);

        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(GooglePublicKeys.class))
                .willReturn(Mono.error(new RuntimeException("Connection failed")));

        // when & then
        assertThatThrownBy(() -> googleOAuthService.verifyAndGetUserInfo(idToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.GOOGLE_KEY_FETCH_FAILED);
    }

    @Test
    @DisplayName("매칭되는 공개키가 없으면 AUTH_301 예외 발생")
    void verifyAndGetUserInfo_noMatchingKey_throwsException() {
        // given
        String idToken = createValidIdToken("test-sub-123", TEST_WEB_CLIENT_ID);
        GooglePublicKeys emptyKeys = new GooglePublicKeys();
        ReflectionTestUtils.setField(emptyKeys, "keys", List.of());

        setupWebClientGetMock(emptyKeys);

        // when & then
        assertThatThrownBy(() -> googleOAuthService.verifyAndGetUserInfo(idToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.GOOGLE_INVALID_TOKEN);
    }

    private String createValidIdToken(String sub, String audience) {
        return createValidIdToken(sub, audience, null);
    }

    private String createValidIdToken(String sub, String audience, String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 3600000); // 1시간 후

        var builder = Jwts.builder()
                .header()
                .keyId(TEST_KID)
                .and()
                .issuer(GOOGLE_ISSUER)
                .audience().add(audience).and()
                .subject(sub)
                .issuedAt(now)
                .expiration(expiration);

        if (email != null) {
            builder.claim("email", email);
        }

        return builder.signWith(testKeyPair.getPrivate())
                .compact();
    }

    private String createExpiredIdToken(String sub) {
        Date past = new Date(System.currentTimeMillis() - 7200000); // 2시간 전
        Date expiration = new Date(System.currentTimeMillis() - 3600000); // 1시간 전 만료

        return Jwts.builder()
                .header()
                .keyId(TEST_KID)
                .and()
                .issuer(GOOGLE_ISSUER)
                .audience().add(TEST_WEB_CLIENT_ID).and()
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
                .issuer(GOOGLE_ISSUER)
                .audience().add(TEST_WEB_CLIENT_ID).and()
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
                .audience().add(TEST_WEB_CLIENT_ID).and()
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
                .issuer(GOOGLE_ISSUER)
                .audience().add("invalid.client.id").and()
                .subject(sub)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(testKeyPair.getPrivate())
                .compact();
    }

    private GooglePublicKeys createMockGooglePublicKeys() {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) testKeyPair.getPublic();

        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(rsaPublicKey.getPublicExponent().toByteArray());

        GooglePublicKeys.Key key = new GooglePublicKeys.Key();
        ReflectionTestUtils.setField(key, "kty", "RSA");
        ReflectionTestUtils.setField(key, "kid", TEST_KID);
        ReflectionTestUtils.setField(key, "use", "sig");
        ReflectionTestUtils.setField(key, "alg", "RS256");
        ReflectionTestUtils.setField(key, "n", n);
        ReflectionTestUtils.setField(key, "e", e);

        GooglePublicKeys keys = new GooglePublicKeys();
        ReflectionTestUtils.setField(keys, "keys", List.of(key));

        return keys;
    }

    private void setupWebClientGetMock(GooglePublicKeys mockKeys) {
        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(GooglePublicKeys.class)).willReturn(Mono.just(mockKeys));
    }
}
