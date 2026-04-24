package com.nexters.sseotdabwa.api.feeds.dto;

import com.nexters.sseotdabwa.common.validation.ValidUrl;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record FeedCreateRequestV2(

        @NotNull(message = "카테고리는 필수입니다.")
        FeedCategory category,

        @NotNull(message = "가격은 필수 입력입니다.")
        @Positive(message = "가격은 1 이상이어야 합니다.")
        Long price,

        @Size(max = 100, message = "내용은 100자 이하로 입력해주세요.")
        String content,

        @NotEmpty(message = "이미지는 최소 1장 필요합니다.")
        @Size(max = 3, message = "이미지는 최대 3장까지 가능합니다.")
        List<@Valid ImageRequest> images,

        @ValidUrl
        String link,

        @Size(max = 40, message = "제목은 40자 이하로 입력해주세요.")
        String title
) {
    public record ImageRequest(
            @NotBlank(message = "이미지 s3ObjectKey는 필수입니다.")
            String s3ObjectKey,

            @NotNull(message = "이미지 너비는 필수입니다.")
            @Positive(message = "이미지 너비는 1 이상이어야 합니다.")
            Integer imageWidth,

            @NotNull(message = "이미지 높이는 필수입니다.")
            @Positive(message = "이미지 높이는 1 이상이어야 합니다.")
            Integer imageHeight
    ) {}
}
