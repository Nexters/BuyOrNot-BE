package com.nexters.sseotdabwa.domain.notifications.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nexters.sseotdabwa.domain.notifications.entity.Notification;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;

import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserIdAndFeedIdAndType(Long userId, Long feedId, NotificationType type);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    // type 없는 케이스
    @Query("""
        select n
        from Notification n
        where n.user.id = :userId
          and n.createdAt >= :cutoff
        order by n.createdAt desc
    """)
    List<Notification> findRecentByUser(
            @Param("userId") Long userId,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    // type 있는 케이스
    @Query("""
        select n
        from Notification n
        where n.user.id = :userId
          and n.createdAt >= :cutoff
          and n.type = :type
        order by n.createdAt desc
    """)
    List<Notification> findRecentByUserAndType(
            @Param("userId") Long userId,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("type") NotificationType type,
            Pageable pageable
    );

    void deleteByFeedId(Long feedId);
}
