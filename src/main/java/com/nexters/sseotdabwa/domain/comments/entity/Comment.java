package com.nexters.sseotdabwa.domain.comments.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * 비회원(게스트) 작성 댓글의 닉네임. 회원 작성 댓글은 null.
     */
    @Column(name = "guest_nickname", length = 40)
    private String guestNickname;

    /**
     * 비회원(게스트) 작성 댓글의 표시용 프로필 이미지 URL. 작성 시점에 한 번 랜덤 부여되어 고정됨. 회원 작성 댓글은 null.
     */
    @Column(name = "guest_profile_image")
    private String guestProfileImage;

    /**
     * 작성 시점 닉네임 스냅샷(회원/게스트 공용). 회원 닉네임이 나중에 바뀌어도 과거 댓글 표시는 불변.
     * 같은 피드 안에서의 닉네임 중복 검사도 이 컬럼 기준으로 한다.
     */
    @Column(name = "display_nickname", nullable = false, length = 40)
    private String displayNickname;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Builder
    public Comment(Feed feed, User user, String guestNickname, String guestProfileImage, String displayNickname, String content) {
        this.feed = feed;
        this.user = user;
        this.guestNickname = guestNickname;
        this.guestProfileImage = guestProfileImage;
        this.displayNickname = displayNickname;
        this.content = content;
    }

    public boolean isGuestComment() {
        return this.user == null;
    }
}
