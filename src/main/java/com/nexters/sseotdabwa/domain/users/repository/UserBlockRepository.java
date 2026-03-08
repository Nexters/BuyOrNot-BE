package com.nexters.sseotdabwa.domain.users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexters.sseotdabwa.domain.users.entity.UserBlock;

/**
 * 사용자 차단 관계 Repository
 */
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByUserIdAndBlockedUserId(Long userId, Long blockedUserId);

    Optional<UserBlock> findByUserIdAndBlockedUserId(Long userId, Long blockedUserId);

    List<UserBlock> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteAllByUserId(Long userId);

    void deleteAllByBlockedUserId(Long userId);
}
