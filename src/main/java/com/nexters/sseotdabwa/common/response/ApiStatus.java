package com.nexters.sseotdabwa.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiStatus {
    SUCCESS("SUCCESS"),
    ERROR("ERROR"); // 나중에 GlobalExceptionHandler 붙일 때 사용

    private final String value;
}
