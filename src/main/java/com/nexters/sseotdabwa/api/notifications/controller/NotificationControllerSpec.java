package com.nexters.sseotdabwa.api.notifications.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.notifications.dto.NotificationResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.users.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications", description = "알림 API")
public interface NotificationControllerSpec {

    @Operation(
            summary = "알림 리스트 조회",
            description = """
                최근 30일 이내 알림을 최신순으로 조회합니다.

                - type 파라미터로 필터 가능 (MY_FEED_CLOSED / PARTICIPATED_FEED_CLOSED)
                - resultPercent : 승리한 쪽의 퍼센트 (0~100)
                            - resultLabel :
                                - YES  : 찬성이 더 많음
                                - NO   : 반대가 더 많음
                                - TIE  : 동률
                                - ZERO : 투표 0건
                """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping
    ApiResponse<List<NotificationResponse>> getNotifications(
            @Parameter(hidden = true) User user,
            @RequestParam(required = false) NotificationType type
    );

    @Operation(
            summary = "알림 읽음 처리",
            description = """
                알림을 읽음 처리합니다.
                - 본인 알림만 가능
                - idempotent
                """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PatchMapping("/{notificationId}/read")
    ApiResponse<Void> readNotification(
            @Parameter(hidden = true) User user,
            @PathVariable Long notificationId
    );
}
