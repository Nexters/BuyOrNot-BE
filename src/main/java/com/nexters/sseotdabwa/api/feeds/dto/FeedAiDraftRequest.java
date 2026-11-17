package com.nexters.sseotdabwa.api.feeds.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record FeedAiDraftRequest(

        @Schema(description = "presigned 업로드로 올린 이미지 s3ObjectKey 목록 (1~3장, feeds/ 경로)", example = "[\"feeds/9f1c..._photo.jpg\"]")
        @NotEmpty(message = "이미지는 최소 1장 필요합니다.")
        @Size(max = 3, message = "이미지는 최대 3장까지 가능합니다.")
        List<@NotBlank(message = "이미지 s3ObjectKey는 필수입니다.") String> s3ObjectKeys
) {
}
