package com.nexters.sseotdabwa.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * URL 유효성 검증기
 * - http/https 스킴만 허용
 * - 호스트에 점(.)이 포함된 유효한 도메인 필수 (https://foo 같은 호스트 없는 URL 거부)
 * - 도메인에 한글 문자 불허
 * - 공백 불허
 */
public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://" +                                          // http:// 또는 https://
            "[a-zA-Z0-9][a-zA-Z0-9\\-]*(\\.[a-zA-Z0-9\\-]+)+" +  // 점이 포함된 호스트 필수 (e.g. example.com)
            "(:\\d{1,5})?" +                                        // 포트 (선택)
            "([/?#][^\\s]*)?" +                                     // 경로/쿼리/프래그먼트 (선택)
            "$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // null/blank는 통과 (필수 여부는 @NotBlank로 제어)
        }
        if (value.contains(" ")) {
            return false;
        }
        return URL_PATTERN.matcher(value).matches();
    }
}
