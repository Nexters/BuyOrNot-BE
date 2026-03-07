package com.nexters.sseotdabwa.domain.users.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.users.exception.UserErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.entity.UserBlock;
import com.nexters.sseotdabwa.domain.users.repository.UserBlockRepository;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 차단 도메인 서비스
 * - UserBlock 생성/조회/삭제 등 "차단 관계" 자체의 핵심 로직만 담당
 *
 * 정책:
 * - 자기 자신 차단 불가
 * - 동일 대상 중복 차단 불가 (exists로 1차 방어, DB unique로 최종 방어)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;

    /**
     * 사용자 차단
     */
    @Transactional
    public void blockUser(User user, User targetUser) {

        if (user.getId().equals(targetUser.getId())) {
            throw new GlobalException(UserErrorCode.BLOCK_SELF_NOT_ALLOWED);
        }

        if (userBlockRepository.existsByUserIdAndBlockedUserId(user.getId(), targetUser.getId())) {
            throw new GlobalException(UserErrorCode.ALREADY_BLOCKED_USER);
        }

        UserBlock block = UserBlock.builder()
                .user(user)
                .blockedUser(targetUser)
                .build();

        userBlockRepository.save(block);
    }
    /**
     * 차단 목록 조회 (최신순)
     */
    public List<UserBlock> findMyBlocks(Long userId) {
        return userBlockRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 사용자 차단 해제
     */
    @Transactional
    public void unblock(User user, Long targetUserId) {
        UserBlock relation = userBlockRepository.findByUserIdAndBlockedUserId(user.getId(), targetUserId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.BLOCK_USER_NOT_FOUND));

        userBlockRepository.delete(relation);
    }
}
