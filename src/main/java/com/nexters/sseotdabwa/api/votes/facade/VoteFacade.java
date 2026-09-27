package com.nexters.sseotdabwa.api.votes.facade;

import java.util.List;

import com.nexters.sseotdabwa.api.votes.dto.VoteRequest;
import com.nexters.sseotdabwa.api.votes.dto.VoteResponse;
import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.service.FeedImageService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.enums.VoteType;
import com.nexters.sseotdabwa.domain.votes.event.VoteCreatedEvent;
import com.nexters.sseotdabwa.domain.votes.exception.VoteErrorCode;
import com.nexters.sseotdabwa.domain.votes.service.VoteLogService;
import com.nexters.sseotdabwa.domain.votes.service.command.VoteCreateCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteFacade {

    private final FeedService feedService;
    private final FeedImageService feedImageService;
    private final VoteLogService voteLogService;
    private final AwsProperties awsProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public VoteResponse vote(User user, Long feedId, VoteRequest request) {
        Feed feed = feedService.findByIdWithLock(feedId);

        if (feed.isExpired() || !feed.isVoteOpen()) {
            throw new GlobalException(VoteErrorCode.VOTE_FEED_CLOSED);
        }
        if (feed.isOwner(user)) {
            throw new GlobalException(VoteErrorCode.VOTE_OWN_FEED);
        }
        if (voteLogService.existsByUserAndFeed(user.getId(), feedId)) {
            throw new GlobalException(VoteErrorCode.VOTE_ALREADY_VOTED);
        }

        VoteChoice choice = request.choice();
        if (choice == VoteChoice.YES) {
            feed.incrementYes();
        } else {
            feed.incrementNo();
        }

        VoteCreateCommand command = new VoteCreateCommand(user, feed, choice, VoteType.USER);
        voteLogService.createVoteLog(command);

        eventPublisher.publishEvent(new VoteCreatedEvent(feed));

        return VoteResponse.of(feed, choice, user.getProfileImage(), buildFirstImageUrl(feed));
    }

    @Transactional
    public VoteResponse guestVote(Long feedId, VoteRequest request) {
        Feed feed = feedService.findByIdWithLock(feedId);

        if (feed.isExpired() || !feed.isVoteOpen()) {
            throw new GlobalException(VoteErrorCode.VOTE_FEED_CLOSED);
        }

        VoteChoice choice = request.choice();
        if (choice == VoteChoice.YES) {
            feed.incrementYes();
        } else {
            feed.incrementNo();
        }

        VoteCreateCommand command = new VoteCreateCommand(null, feed, choice, VoteType.SYSTEM);
        voteLogService.createVoteLog(command);

        return VoteResponse.of(feed, choice, null, buildFirstImageUrl(feed));
    }

    /**
     * 투표 완료 스낵바에 노출할 피드 대표(첫번째) 이미지 URL
     */
    private String buildFirstImageUrl(Feed feed) {
        List<FeedImage> images = feedImageService.findByFeed(feed);
        if (images.isEmpty()) {
            return null;
        }
        final String domain = awsProperties.cloudfront().domain().replaceAll("/$", "");
        return domain + "/" + images.get(0).getS3ObjectKey();
    }
}
