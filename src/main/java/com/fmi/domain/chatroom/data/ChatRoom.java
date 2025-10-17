package com.fmi.domain.chatroom.data;

import com.fmi.domain.User;
import com.fmi.domain.post.data.Post;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "user_id", nullable = false)
    //private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(length = 100)
    private String lastMessage;

    // 채팅방 생성 시간
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 마지막 메시지가 온 시간
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatRoomParticipant> participants = new ArrayList<>();

    public void addParticipant(ChatRoomParticipant participant) {
        this.participants.add(participant);
        participant.setChatRoom(this);
    }

    //나를 제외한 다른 참여자를 찾기
    public User getOtherParticipant(Long userId) {
        return this.participants.stream()
                .map(ChatRoomParticipant::getUser) // 실제 User 객체를 꺼냅니다.
                .filter(user -> !user.getId().equals(userId)) // 나 자신이 아닌 사용자를 필터링
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("채팅방에 다른 참여자가 없습니다."));
    }

    public void updateLastMessage(String content) {
        this.lastMessage = content;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 사용자가 채팅방의 참여자인지 확인하는 메서드
     */
    public boolean isParticipant(User user) {
        return this.participants.stream()
                .anyMatch(p -> p.getUser().getId().equals(user.getId()));
    }

}
