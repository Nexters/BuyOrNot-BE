package com.nexters.sseotdabwa.domain.users.service;

import java.util.Set;
import java.util.regex.Pattern;

import com.nexters.sseotdabwa.api.users.exception.UserErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.comments.service.CommentProfanityFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * 회원이 직접 입력하는 닉네임(가입 시 최초 설정 / 프로필 수정)의 형식·금칙어 검증.
 * {@link RandomNicknameGenerator}는 자동 생성 포맷({형용사}{명사}_{4자리숫자}) 검증 전용이라 재사용 불가 — 별도로 둔다.
 * 중복 검사(USER_011)는 DB 조회가 필요해 {@link UserService}에서 이 클래스의 두 검증 사이에 수행한다.
 * 욕설 검사는 댓글 금칙어 필터링에 쓰는 {@link CommentProfanityFilter}(io.github.lisuugi:korean-profanity-filter)를 그대로 재사용한다.
 */
@Component
@RequiredArgsConstructor
public class NicknameValidator {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 10;

    private static final Pattern ALLOWED_CHARSET = Pattern.compile("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ]+$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");
    private static final Pattern STANDALONE_JAMO = Pattern.compile(".*[ㄱ-ㅎㅏ-ㅣ].*");

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "관리자", "운영자", "공식", "admin", "administrator", "official"
    );

    private final CommentProfanityFilter commentProfanityFilter;

    /**
     * 우선순위: 특수문자/이모지 → 중간 띄어쓰기 → 글자 수 → 자음모음단독/숫자전용. 앞뒤 공백은 호출 전 trim되어 있어야 한다.
     */
    public void validateFormat(String trimmed) {
        if (!ALLOWED_CHARSET.matcher(trimmed).matches()) {
            throw new GlobalException(UserErrorCode.NICKNAME_SPECIAL_CHARACTER);
        }
        if (trimmed.contains(" ")) {
            throw new GlobalException(UserErrorCode.NICKNAME_WHITESPACE);
        }
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new GlobalException(UserErrorCode.NICKNAME_LENGTH_INVALID);
        }
        if (DIGITS_ONLY.matcher(trimmed).matches() || STANDALONE_JAMO.matcher(trimmed).find()) {
            throw new GlobalException(UserErrorCode.NICKNAME_INVALID_COMPOSITION);
        }
    }

    /**
     * 사칭 키워드 + 욕설(금칙어) 검사. 사칭 키워드를 먼저 본 뒤 욕설 필터를 태운다 — 둘 다 같은 에러코드(USER_012)로 반환.
     */
    public void validateNotForbidden(String trimmed) {
        String lower = trimmed.toLowerCase();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                throw new GlobalException(UserErrorCode.NICKNAME_FORBIDDEN_WORD);
            }
        }
        if (commentProfanityFilter.containsProfanity(trimmed)) {
            throw new GlobalException(UserErrorCode.NICKNAME_FORBIDDEN_WORD);
        }
    }
}
