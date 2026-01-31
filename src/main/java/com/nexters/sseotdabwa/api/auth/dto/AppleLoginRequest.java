package com.nexters.sseotdabwa.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Apple 로그인 요청 DTO
 * - iOS/Web에서 Apple SDK로 로그인 후 받은 Authorization Code 전달
 * - redirectUri: Web 로그인 시 필수 (초기 인증 요청과 동일한 값)
 *                iOS 네이티브 앱 (ASAuthorizationAppleIDProvider)은 불필요
 */
@Schema(description = "Apple 소셜 로그인 요청")
public record AppleLoginRequest(
        @Schema(description = "Apple SDK로 발급받은 Authorization Code", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Apple Authorization Code는 필수입니다.")
        String authorizationCode,

        @Schema(description = "Web 로그인 시 사용된 redirect_uri (iOS 네이티브 앱은 생략 가능)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String redirectUri
) {
}
