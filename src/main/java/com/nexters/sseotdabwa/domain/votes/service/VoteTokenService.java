package com.nexters.sseotdabwa.domain.votes.service;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 게스트가 특정 피드에 투표했음을 증명하는 단기 서명 토큰(voteToken) 발급/검증
 * - 게스트는 세션/디바이스ID가 없어 "투표한 그 게스트인지" DB로 식별할 방법이 없음
 * - 투표 성공 시 이 토큰을 발급해 댓글 작성 시 제출받아, 서명 + feedId 일치 + 만료만 확인한다(stateless, DB 조회 없음)
 * - 만료 시각은 해당 피드의 투표 마감 시각(48시간)과 동일하게 맞춘다
 */
@Service
public class VoteTokenService {

    private static final String PURPOSE_CLAIM = "purpose";
    private static final String PURPOSE_GUEST_COMMENT = "guest_comment";

    private final SecretKey secretKey;

    public VoteTokenService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createGuestVoteToken(Feed feed) {
        Date expiryDate = Date.from(feed.getVoteClosedAt().atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .subject(String.valueOf(feed.getId()))
                .claim(PURPOSE_CLAIM, PURPOSE_GUEST_COMMENT)
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * voteToken이 주어진 feedId에 대해 유효한지 검증 (서명, purpose, feedId 일치, 만료 여부)
     */
    public boolean isValid(String token, Long feedId) {
        if (token == null || token.isBlank() || feedId == null) {
            return false;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String purpose = claims.get(PURPOSE_CLAIM, String.class);
            Long tokenFeedId = Long.parseLong(claims.getSubject());
            return PURPOSE_GUEST_COMMENT.equals(purpose) && feedId.equals(tokenFeedId);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
