package com.nexters.sseotdabwa.domain.users.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    public static final String NICKNAME_UNIQUE_CONSTRAINT = "uk_users_nickname";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String socialId;

    @Column
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialAccount socialAccount;

    private String profileImage;

    @Column
    private String email;

    @Column(length = 255)
    private String fcmToken;

    @Column(nullable = false)
    private boolean pushEnabled;

    @Column
    private LocalDateTime lastOpenedAt;

    @Column
    private LocalDateTime nicknameUpdatedAt;

    /**
     * 이메일 알림 수신 여부 (푸시를 받을 수 없는 웹 회원 대상). 메일 하단 "이메일 알림 끄기"로 false가 된다.
     * pushEnabled와 분리 — 나중에 앱을 설치해도 푸시까지 꺼져 있지 않도록.
     */
    @Column(nullable = false)
    private boolean emailNotificationEnabled;

    @Builder
    public User(String socialId, String nickname, SocialAccount socialAccount,
            String profileImage, String email) {
        this.socialId = socialId;
        this.nickname = nickname;
        this.socialAccount = socialAccount;
        this.profileImage = profileImage;
        this.email = email;
        this.pushEnabled = true;
        this.emailNotificationEnabled = true;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateProfile(String nickname, String profileImage) {
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    /**
     * 닉네임만 변경하며 변경 시각을 함께 기록한다 (20일 쿨다운 계산용).
     * 프로필 이미지만 바꾸는 경우와 구분하기 위해 updateProfile과 별도로 둔다.
     */
    public void updateNickname(String nickname, LocalDateTime updatedAt) {
        this.nickname = nickname;
        this.nicknameUpdatedAt = updatedAt;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = (fcmToken == null) ? null : fcmToken.trim();
    }

    public void updateLastOpenedAt() {
        this.lastOpenedAt = LocalDateTime.now();
    }

    public boolean canReceivePush() {
        return this.pushEnabled && this.fcmToken != null && !this.fcmToken.isBlank();
    }

    /**
     * 푸시를 받을 수 없는(FCM 토큰 없는) 웹 회원 중, 알림을 끄지 않았고 이메일이 있는 경우
     */
    public boolean canReceiveEmail() {
        return !canReceivePush()
                && this.pushEnabled
                && this.emailNotificationEnabled
                && this.email != null && !this.email.isBlank();
    }

    public void disableEmailNotification() {
        this.emailNotificationEnabled = false;
    }
}
