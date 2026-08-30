package com.nexters.sseotdabwa.domain.users.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySocialIdAndSocialAccount(String socialId, SocialAccount socialAccount);

    boolean existsByNickname(String nickname);

    List<User> findByIdIn(List<Long> ids);

    /**
     * 마케팅 푸시 발송 대상 조회
     * - 최근 7일 내 앱 오픈
     * - 투표를 한 번도 등록하지 않은 유저
     * - 가입한지 signupCutoff 이전인 유저만 (가입 초반은 온보딩 유도 푸시가 전담 — 중복 발송 방지)
     * - pushEnabled = true, fcmToken 존재
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.lastOpenedAt >= :cutoff
          AND u.createdAt < :signupCutoff
          AND u.pushEnabled = true
          AND u.fcmToken IS NOT NULL
          AND TRIM(u.fcmToken) <> ''
          AND NOT EXISTS (
              SELECT f FROM Feed f WHERE f.user = u
          )
    """)
    List<User> findMarketingTargets(@Param("cutoff") LocalDateTime cutoff, @Param("signupCutoff") LocalDateTime signupCutoff);

    /**
     * 온보딩 유도 푸시 발송 대상 조회
     * - 가입일이 [rangeStart, rangeEnd) 범위에 속함 (가입 후 정확히 N일 경과)
     * - 피드를 한 번도 등록하지 않은 유저
     * - pushEnabled = true, fcmToken 존재
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.createdAt >= :rangeStart AND u.createdAt < :rangeEnd
          AND u.pushEnabled = true
          AND u.fcmToken IS NOT NULL
          AND TRIM(u.fcmToken) <> ''
          AND NOT EXISTS (
              SELECT f FROM Feed f WHERE f.user = u
          )
    """)
    List<User> findOnboardingTargets(@Param("rangeStart") LocalDateTime rangeStart, @Param("rangeEnd") LocalDateTime rangeEnd);
}
