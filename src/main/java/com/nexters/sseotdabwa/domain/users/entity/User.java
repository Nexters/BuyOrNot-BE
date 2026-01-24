package com.nexters.sseotdabwa.domain.users.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialAccount socialAccount;

    private String profileImage;

    private String fcmToken;

    @Builder
    public User(String email, String nickname, SocialAccount socialAccount, String profileImage, String fcmToken) {
        this.email = email;
        this.nickname = nickname;
        this.socialAccount = socialAccount;
        this.profileImage = profileImage;
        this.fcmToken = fcmToken;
    }
}
