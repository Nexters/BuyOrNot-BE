package com.nexters.sseotdabwa.domain.users.entity;

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

    @Builder
    public User(String socialId, String nickname, SocialAccount socialAccount,
            String profileImage, String email) {
        this.socialId = socialId;
        this.nickname = nickname;
        this.socialAccount = socialAccount;
        this.profileImage = profileImage;
        this.email = email;
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
}
