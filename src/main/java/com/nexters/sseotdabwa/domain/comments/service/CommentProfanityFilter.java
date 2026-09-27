package com.nexters.sseotdabwa.domain.comments.service;

import io.github.lisuugi.ProfanityFilter;

import org.springframework.stereotype.Component;

/**
 * 댓글 내용의 금칙어(욕설) 포함 여부를 검사한다.
 * 실제 사전 매칭/회피 패턴(자모 결합, 특수문자 치환, 반복 문자 등) 정규화는
 * io.github.lisuugi:korean-profanity-filter 라이브러리의 필터 체인이 내부적으로 처리한다.
 */
@Component
public class CommentProfanityFilter {

    private final ProfanityFilter delegate = ProfanityFilter.createDefault();

    public boolean containsProfanity(String content) {
        return delegate.containsWords(content);
    }
}
