package com.nexters.sseotdabwa.domain.auth.service.external;

import lombok.Builder;
import lombok.Getter;

/**
 * Apple 사용자 정보 DTO
 * - Identity Token(JWT)에서 추출한 정보
 */
@Getter
@Builder
public class AppleUserInfo {

    private final String sub;  // Apple User ID
    private final String email;
}
