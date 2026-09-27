package com.nexters.sseotdabwa.api.comments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "게스트 댓글 작성 요청")
public record CommentCreateRequestGuest(
        @Schema(description = "댓글 내용 (공백 제거 후 1~300자)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "댓글 내용은 필수입니다.")
        String content,
        @Schema(
                description = "GET /api/v1/guest/nickname으로 발급받은 닉네임. 실제 형용사+명사 조합이 아니면 서버가 새로 발급해서 대체하고, "
                        + "같은 피드 안에서 이미 쓰이는 닉네임이면 자동으로 재발급한다.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "닉네임은 필수입니다.")
        String guestNickname,
        @Schema(
                description = "삭제 시 본인 확인용 비밀번호. BCrypt 해시로만 저장되며 서버는 복구 수단을 제공하지 않으므로 "
                        + "클라이언트가 반드시 별도로 기억해두어야 한다.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "비밀번호는 필수입니다.")
        String guestPassword
) {}
