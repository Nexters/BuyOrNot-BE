package com.nexters.sseotdabwa.domain.auth.service.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 사용자 정보 API 응답 DTO
 */
@Getter
@NoArgsConstructor
public class KakaoUserInfo {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {

        private String email;
        private Profile profile;

        @Getter
        @NoArgsConstructor
        public static class Profile {

            private String nickname;

            @JsonProperty("profile_image_url")
            private String profileImageUrl;
        }
    }

    public String getEmail() {
        if (kakaoAccount == null) {
            return null;
        }
        return kakaoAccount.getEmail();
    }

    public String getNickname() {
        if (kakaoAccount == null || kakaoAccount.getProfile() == null) {
            return null;
        }
        return kakaoAccount.getProfile().getNickname();
    }

    public String getProfileImage() {
        if (kakaoAccount == null || kakaoAccount.getProfile() == null) {
            return null;
        }
        return kakaoAccount.getProfile().getProfileImageUrl();
    }
}
