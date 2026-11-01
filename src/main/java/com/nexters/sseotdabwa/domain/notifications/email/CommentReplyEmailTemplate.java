package com.nexters.sseotdabwa.domain.notifications.email;

import org.springframework.web.util.HtmlUtils;

/**
 * 답글 알림 메일 HTML
 * - 메일 클라이언트 호환을 위해 table 레이아웃 + 인라인 CSS만 사용 (외부 CSS/폰트/이미지 없음)
 * - 닉네임/피드 제목/답글 내용은 사용자 입력이므로 반드시 escape
 */
public final class CommentReplyEmailTemplate {

    private static final int CONTENT_PREVIEW_MAX_LENGTH = 100;

    private CommentReplyEmailTemplate() {
    }

    public static String render(String headline, String feedTitle, String replierNickname, String replyContent,
                                String feedUrl, String unsubscribeUrl) {
        String safeHeadline = escape(headline);
        String safeFeedTitle = escape(feedTitle == null || feedTitle.isBlank() ? "내가 참여한" : "'" + feedTitle + "'");
        String safeNickname = escape(replierNickname);
        String safeContent = escape(preview(replyContent));
        String safeFeedUrl = escape(feedUrl);
        String safeUnsubscribeUrl = escape(unsubscribeUrl);

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>%1$s</title></head>
                <body style="margin:0;padding:0;background-color:#f4f5f7;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f4f5f7;">
                  <tr><td align="center" style="padding:32px 16px;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;background-color:#ffffff;border-radius:16px;font-family:-apple-system,BlinkMacSystemFont,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;">
                      <tr><td style="padding:28px 28px 0 28px;font-size:20px;font-weight:800;color:#1f2329;">살까말까</td></tr>
                      <tr><td style="padding:20px 28px 0 28px;font-size:18px;font-weight:700;line-height:1.5;color:#1f2329;">%1$s</td></tr>
                      <tr><td style="padding:8px 28px 0 28px;font-size:14px;line-height:1.5;color:#6b7280;">%2$s 투표에 달린 답글이에요</td></tr>
                      <tr><td style="padding:20px 28px 0 28px;">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f4f5f7;border-radius:12px;">
                          <tr><td style="padding:16px 18px 4px 18px;font-size:14px;font-weight:700;color:#1f2329;">%3$s</td></tr>
                          <tr><td style="padding:0 18px 16px 18px;font-size:15px;line-height:1.6;color:#374151;word-break:break-all;">%4$s</td></tr>
                        </table>
                      </td></tr>
                      <tr><td align="center" style="padding:24px 28px 28px 28px;">
                        <a href="%5$s" target="_blank" style="display:inline-block;padding:14px 32px;background-color:#1f2329;border-radius:12px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">답글 보러가기</a>
                      </td></tr>
                    </table>
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;font-family:-apple-system,BlinkMacSystemFont,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;">
                      <tr><td style="padding:16px 8px 0 8px;font-size:12px;line-height:1.6;color:#9ca3af;">
                        살까말까 댓글 알림 메일입니다. 이 메일은 발신 전용이라 회신할 수 없어요.<br>
                        더 이상 받고 싶지 않다면 <a href="%6$s" target="_blank" style="color:#9ca3af;text-decoration:underline;">이메일 알림 끄기</a>
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(safeHeadline, safeFeedTitle, safeNickname, safeContent, safeFeedUrl, safeUnsubscribeUrl);
    }

    private static String preview(String content) {
        if (content == null) return "";
        return content.length() > CONTENT_PREVIEW_MAX_LENGTH
                ? content.substring(0, CONTENT_PREVIEW_MAX_LENGTH) + "..."
                : content;
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
