package com.nexters.sseotdabwa.api.notifications.facade;

import java.util.List;

import org.springframework.stereotype.Component;
import com.nexters.sseotdabwa.domain.notifications.service.NotificationService;

import lombok.RequiredArgsConstructor;

/**
 * 알림 관련 흐름을 조합하는 Facade
 */
@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;

}
