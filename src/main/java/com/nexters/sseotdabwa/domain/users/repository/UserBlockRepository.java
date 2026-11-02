package com.nexters.sseotdabwa.domain.users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexters.sseotdabwa.domain.users.entity.UserBlock;

/**
 * 사용자 차단 관계 Repository
 */
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByUserIdAndBlockedUserId(Long userId, Long blockedUserId);

    Optional<UserBlock> findByUserIdAndBlockedUserId(Long userId, Long blockedUserId);

    @EntityGraph(attributePaths = "blockedUser")
    List<UserBlock> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT ub.blockedUser.id FROM UserBlock ub WHERE ub.user.id = :userId")
    List<Long> findBlockedUserIdsByUserId(@Param("userId") Long userId);

    /**
     * 해당 유저를 차단한 유저 id 목록 (답글 알림에서 차단한 사람의 답글 알림을 받지 않도록 제외할 때 사용)
     */
    @Query("SELECT ub.user.id FROM UserBlock ub WHERE ub.blockedUser.id = :blockedUserId")
    List<Long> findBlockerIdsByBlockedUserId(@Param("blockedUserId") Long blockedUserId);

    void deleteAllByUserId(Long userId);

    void deleteAllByBlockedUserId(Long userId);
}
