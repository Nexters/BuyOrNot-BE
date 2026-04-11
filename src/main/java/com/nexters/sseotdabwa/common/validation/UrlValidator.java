package com.nexters.sseotdabwa.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * URL 유효성 검증기
 * - http/https 스킴만 허용
 * - 도메인에 한글 문자 불허
 * - 공백 불허
 */
public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://" +               // http:// 또는 https://
            "[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+" + // ASCII 문자만 허용 (공백, 한글 제외)
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
