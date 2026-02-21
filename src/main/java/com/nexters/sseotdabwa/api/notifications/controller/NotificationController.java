package com.nexters.sseotdabwa.api.notifications.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.nexters.sseotdabwa.api.notifications.dto.NotificationResponse;
import com.nexters.sseotdabwa.api.notifications.facade.NotificationFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.users.entity.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerSpec {

    private final NotificationFacade notificationFacade;

    @Override
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @CurrentUser User user,
            @RequestParam(required = false) NotificationType type
    ) {
        List<NotificationResponse> response = notificationFacade.getRecentNotifications(user, type);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> readNotification(
            @CurrentUser User user,
            @PathVariable Long notificationId
    ) {
        notificationFacade.markAsRead(user, notificationId);
        return ApiResponse.success(HttpStatus.OK);
    }

    @PostMapping("/test-push")
    public ApiResponse<Void> sendTestPush(
            @CurrentUser User user,
            @RequestParam(defaultValue = "테스트 알림!!!!") String title,
            @RequestParam(defaultValue = "푸시가 정상 동작하는지 확인합니다..") String body
    ) {
        notificationFacade.sendTestPushOnly(user, title, body);
        return ApiResponse.success(HttpStatus.OK);
    }
}
