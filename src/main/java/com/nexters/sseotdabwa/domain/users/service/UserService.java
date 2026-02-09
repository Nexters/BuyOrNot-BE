package com.nexters.sseotdabwa.domain.users.service;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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

    private static final int MAX_NICKNAME_RETRY = 5;

    private final UserRepository userRepository;
    private final RandomNicknameGenerator randomNicknameGenerator;
    private final TransactionTemplate requiresNewTransactionTemplate;

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
     * - 랜덤 닉네임과 프로필 이미지 자동 부여
     * - 닉네임 중복 시 재시도 (DB unique constraint로 race condition 방지)
     * - 각 시도는 REQUIRES_NEW 트랜잭션에서 실행되어 실패 시 롤백 후 재시도 가능
     * @throws GlobalException 최대 재시도 횟수 초과 시
     */
    public User createUser(UserCreateCommand command) {
        for (int attempt = 0; attempt < MAX_NICKNAME_RETRY; attempt++) {
            try {
                String nickname = (attempt == 0) ? command.nickname() : randomNicknameGenerator.generate();
                return saveUserInNewTransaction(command, nickname);
            } catch (DataIntegrityViolationException e) {
                if (!isNicknameConflict(e)) {
                    throw e;
                }
                // 닉네임 중복인 경우 다음 시도에서 새 닉네임 생성
            }
        }
        throw new GlobalException(UserErrorCode.NICKNAME_GENERATION_FAILED);
    }

    private User saveUserInNewTransaction(UserCreateCommand command, String nickname) {
        return requiresNewTransactionTemplate.execute(status -> {
            User user = User.builder()
                    .socialId(command.socialId())
                    .nickname(nickname)
                    .socialAccount(command.socialAccount())
                    .profileImage(command.profileImage())
                    .email(command.email())
                    .build();
            return userRepository.save(user);
        });
    }

    private boolean isNicknameConflict(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        if (cause instanceof ConstraintViolationException cve) {
            String constraintName = cve.getConstraintName();
            return User.NICKNAME_UNIQUE_CONSTRAINT.equals(constraintName);
        }
        return false;
    }

    /**
     * 사용자 프로필 업데이트 (소셜 로그인 시 최신 정보 동기화)
     */
    @Transactional
    public void updateProfile(User user, String nickname, String profileImage) {
        user.updateProfile(nickname, profileImage);
    }

    /**
     * 사용자 프로필 이미지만 업데이트 (닉네임은 유지)
     */
    @Transactional
    public void updateProfileImage(User user, String profileImage) {
        user.updateProfileImage(profileImage);
    }

    /**
     * 사용자 삭제
     */
    @Transactional
    public void delete(User user) {
        userRepository.delete(user);
    }

    /**
     * 중복되지 않는 유니크 닉네임 생성
     * @throws GlobalException 최대 재시도 횟수 초과 시
     */
    public String generateUniqueNickname() {
        for (int i = 0; i < MAX_NICKNAME_RETRY; i++) {
            String nickname = randomNicknameGenerator.generate();
            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
        throw new GlobalException(UserErrorCode.NICKNAME_GENERATION_FAILED);
    }
}
