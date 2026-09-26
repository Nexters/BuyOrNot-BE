package com.nexters.sseotdabwa.api.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로필 수정 요청 (닉네임 최초 설정 겸용)")
public record UserProfileUpdateRequest(
        @Schema(description = "닉네임. 미포함/null이면 변경하지 않음. 닉네임이 아직 없는 상태(최초 설정)면 쿨다운 없이 통과, "
                + "이미 있는 상태에서 변경하면 마지막 변경일로부터 20일이 지나야 함")
        String nickname,
        @Schema(description = "프로필 이미지 URL. 미포함/null이면 변경하지 않음. 변경 주기 제한 없음")
        String profileImage
) {
}
