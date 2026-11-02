package com.nexters.sseotdabwa.api.notifications.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.nexters.sseotdabwa.api.notifications.dto.NotificationResponse;
import com.nexters.sseotdabwa.api.notifications.dto.UnreadCountResponse;
import com.nexters.sseotdabwa.api.notifications.facade.NotificationFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.users.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerSpec {

    private final NotificationFacade notificationFacade;

    @Override
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(@CurrentUser User user) {
        return ApiResponse.success(new UnreadCountResponse(notificationFacade.getUnreadCount(user)), HttpStatus.OK);
    }

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

    /**
     * 이메일 알림 끄기 — 메일 하단 링크(GET) / Gmail 원클릭 "구독 취소"(POST, RFC 8058). 인증 불필요(서명 토큰으로 본인 확인)
     * 브라우저에서 바로 열리므로 JSON이 아니라 간단한 안내 HTML을 반환한다.
     */
    @RequestMapping(value = "/email/unsubscribe", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> unsubscribeEmail(@RequestParam(required = false) String token) {
        boolean success = notificationFacade.unsubscribeEmail(token);
        String message = success
                ? "이메일 알림을 껐어요. 앞으로 답글 알림 메일이 발송되지 않아요."
                : "유효하지 않은 링크예요. 메일에 있는 링크를 다시 눌러주세요.";
        String html = """
                <!DOCTYPE html>
                <html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>살까말까</title></head>
                <body style="margin:0;padding:48px 24px;background-color:#f4f5f7;font-family:-apple-system,BlinkMacSystemFont,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;text-align:center;">
                <p style="font-size:20px;font-weight:800;color:#1f2329;">살까말까</p>
                <p style="font-size:16px;line-height:1.6;color:#374151;">%s</p>
                </body></html>
                """.formatted(message);
        return ResponseEntity.status(success ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(html);
    }
}
