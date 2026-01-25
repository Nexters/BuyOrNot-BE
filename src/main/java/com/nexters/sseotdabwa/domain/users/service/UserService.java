package com.nexters.sseotdabwa.domain.users.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.users.exception.UserErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.users.service.command.UserCreateCommand;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 도메인 서비스
 * - 사용자 조회, 생성, 프로필 업데이트 등 핵심 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 소셜 ID와 소셜 계정 타입으로 사용자 조회
     */
    public Optional<User> findBySocialIdAndProvider(String socialId, SocialAccount provider) {
        return userRepository.findBySocialIdAndSocialAccount(socialId, provider);
    }

    /**
     * ID로 사용자 조회 (없으면 예외 발생)
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }

    /**
     * 신규 사용자 생성 (소셜 로그인 시)
     */
    @Transactional
    public User createUser(UserCreateCommand command) {
        User user = User.builder()
                .socialId(command.socialId())
                .email(command.email())
                .nickname(command.nickname())
                .socialAccount(command.socialAccount())
                .profileImage(command.profileImage())
                .build();
        return userRepository.save(user);
    }

    /**
     * 사용자 프로필 업데이트 (소셜 로그인 시 최신 정보 동기화)
     */
    @Transactional
    public void updateProfile(User user, String nickname, String profileImage) {
        user.updateProfile(nickname, profileImage);
    }
}
