package com.nexters.sseotdabwa.api.comments.facade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.nexters.sseotdabwa.api.comments.dto.CommentPreviewResponse;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.service.CommentAggregate;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.service.FeedUserKey;
import com.nexters.sseotdabwa.domain.votes.service.VoteLogService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * feedId별 "가장 최근 댓글 1개" 미리보기 응답을 배치로 구성한다 (N+1 방지).
 * FeedFacade/UserFacade가 피드 목록/상세 응답에 commentCount/latestComment를 얹을 때 공용으로 재사용한다.
 */
@Component
@RequiredArgsConstructor
public class CommentPreviewAssembler {

    private static final CommentAggregate EMPTY_COMMENT_AGGREGATE = new CommentAggregate(0L, null);

    private final VoteLogService voteLogService;

    public Map<Long, CommentPreviewResponse> build(List<Long> feedIds, Map<Long, CommentAggregate> aggregates) {
        List<Comment> latestComments = feedIds.stream()
                .map(id -> aggregates.getOrDefault(id, EMPTY_COMMENT_AGGREGATE).latestComment())
                .filter(Objects::nonNull)
                .toList();

        List<Long> memberAuthorIds = latestComments.stream()
                .filter(c -> !c.isGuestComment())
                .map(c -> c.getUser().getId())
                .distinct()
                .toList();

        Map<FeedUserKey, VoteChoice> choiceMap = voteLogService.findChoicesByFeedIdsAndUserIds(feedIds, memberAuthorIds);

        Map<Long, CommentPreviewResponse> previews = new HashMap<>();
        for (Long feedId : feedIds) {
            Comment latest = aggregates.getOrDefault(feedId, EMPTY_COMMENT_AGGREGATE).latestComment();
            if (latest == null) {
                continue;
            }
            String profileImage = latest.isGuestComment() ? latest.getGuestProfileImage() : latest.getUser().getProfileImage();
            VoteChoice choice = latest.isGuestComment() ? null : choiceMap.get(new FeedUserKey(feedId, latest.getUser().getId()));
            previews.put(feedId, CommentPreviewResponse.of(latest, profileImage, choice));
        }
        return previews;
    }
}
