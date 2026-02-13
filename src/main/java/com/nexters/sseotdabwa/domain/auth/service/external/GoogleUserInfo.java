package com.nexters.sseotdabwa.domain.auth.service.external;

import lombok.Builder;
import lombok.Getter;

/**
 * Google 사용자 정보 DTO
 * - ID Token(JWT)에서 추출한 정보
 */
@Getter
@Builder
public class GoogleUserInfo {

    private final String sub;  // Google User ID
    private final String email;
}
