package com.nexters.sseotdabwa.domain.notifications.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nexters.sseotdabwa.domain.notifications.entity.Notification;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserIdAndFeedIdAndType(Long userId, Long feedId, NotificationType type);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Query("""
        select n
        from Notification n
        where n.user.id = :userId
          and n.createdAt >= :cutoff
          and (:type is null or n.type = :type)
        order by n.createdAt desc
    """)
    List<Notification> findRecentByUser(Long userId, LocalDateTime cutoff, NotificationType type, Pageable pageable);
}
