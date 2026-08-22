package com.fmi.domain.chatroom.data;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = true)
    private Post post;

    // 채팅방 생성 시간
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Long sourcePostId;

    private String sourceAddress;

    private String sourceTitle;

    @Enumerated(EnumType.STRING)
    private PostType sourcePostType;

    @Enumerated(EnumType.STRING)
    private PostStatus sourcePostStatus;

    private String sourceThumbnailUrl;

    private boolean sourcePostDeleted;

    @Enumerated(EnumType.STRING)
    private Category sourceCategory;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatRoomParticipant> participants = new ArrayList<>();

    public void addParticipant(ChatRoomParticipant participant) {
        this.participants.add(participant);
        participant.setChatRoom(this);
    }

    public void initFromPost(Post post) {
        this.sourcePostId = post.getId();
        this.sourceTitle = post.getTitle();
        this.sourceAddress = post.getAddress();
        this.sourcePostType = post.getPostType();
        this.sourceCategory = post.getCategory();
        this.sourcePostStatus = post.getPostStatus();
    }

    public void snapshotFromPost(Post post, String thumbnailUrl) {
        this.sourcePostId = post.getId();
        this.sourceTitle = post.getTitle();
        this.sourceAddress = post.getAddress();
        this.sourcePostType = post.getPostType();
        this.sourceCategory = post.getCategory();
        this.sourcePostStatus = post.getPostStatus();
        this.sourceThumbnailUrl = thumbnailUrl;
        this.sourcePostDeleted = true;
    }

    public void clearPostReference() {
        this.post = null;
    }

    // 나를 제외한 다른 참여자를 찾기
    public User getOtherParticipant(Long userId) {
        return this.participants.stream()
                .map(ChatRoomParticipant::getUser) // 실제 User 객체를 꺼냅니다.
                .filter(user -> !user.getId().equals(userId)) // 나 자신이 아닌 사용자를 필터링
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_INVALID_STATE));
    }

    /**
     * 특정 유저의 참여 정보(participant)를 가져오기
     */
    public ChatRoomParticipant getParticipant(Long userId) {
        return this.participants.stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_ACCESS_DENIED));
    }

    /**
     * 사용자가 채팅방의 참여자인지 확인하는 메서드
     */
    public boolean isParticipant(User user) {
        return this.participants.stream().anyMatch(p -> p.getUser().getId().equals(user.getId()));
    }
}
