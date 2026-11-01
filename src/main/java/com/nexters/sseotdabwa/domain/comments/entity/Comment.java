package com.nexters.sseotdabwa.domain.comments.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "feed_comments",
        indexes = {
                @Index(name = "idx_feed_comments_parent_id", columnList = "parent_id, id")
        }
)
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
     * 답글이면 부모(원댓글), 원댓글이면 null. 답글의 부모는 항상 원댓글이다(답글에 답글 불가, depth 1).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_id", nullable = true)
    private Comment parent;

    /**
     * 비회원(게스트) 작성 댓글의 닉네임. 회원 작성 댓글은 null.
     */
    @Column(name = "guest_nickname", length = 40)
    private String guestNickname;

    /**
     * 비회원(게스트) 작성 댓글 삭제 시 본인 확인용 비밀번호 해시. 회원 작성 댓글은 null.
     */
    @Column(name = "guest_password_hash")
    private String guestPasswordHash;

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

    /**
     * 길이를 지정하지 않으면 Hibernate 6가 @Lob String을 길이 255로 보고 MySQL tinytext(최대 255바이트)로 매핑한다.
     * 한글은 1자 3바이트라 약 85자를 넘으면 저장이 실패하므로, 300자 제한을 담을 수 있게 text로 매핑한다.
     * (DB: ALTER TABLE feed_comments MODIFY COLUMN content TEXT ... — prod validate와 맞추기 위해 함께 반영)
     */
    @Lob
    @Column(name = "content", nullable = false, length = 65535)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false)
    private ReportStatus reportStatus;

    @Builder
    public Comment(Feed feed, User user, Comment parent, String guestNickname, String guestPasswordHash,
                   String guestProfileImage, String displayNickname, String content) {
        this.feed = feed;
        this.user = user;
        this.parent = parent;
        this.guestNickname = guestNickname;
        this.guestPasswordHash = guestPasswordHash;
        this.guestProfileImage = guestProfileImage;
        this.displayNickname = displayNickname;
        this.content = content;
        this.reportStatus = ReportStatus.NONE;
    }

    public boolean isGuestComment() {
        return this.user == null;
    }

    public boolean isOwner(User user) {
        return this.user != null && this.user.getId().equals(user.getId());
    }

    /**
     * 댓글 작성자가 이 피드의 작성자 본인인지 (댓글 태그를 "작성자"로 고정 표시하기 위함)
     */
    public boolean isByFeedAuthor() {
        return !isGuestComment() && this.feed.getUser() != null && this.feed.getUser().getId().equals(this.user.getId());
    }

    public void report() {
        this.reportStatus = ReportStatus.REPORTED;
    }

    public boolean isReported() {
        return this.reportStatus == ReportStatus.REPORTED;
    }

    public boolean isReply() {
        return this.parent != null;
    }
}
