package com.nexters.sseotdabwa.domain.notifications.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nexters.sseotdabwa.domain.notifications.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
