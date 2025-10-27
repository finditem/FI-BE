package com.fmi.domain.chatroom.data;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatroom.data.enums.ParticipantState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ParticipantState participantState = ParticipantState.ACTIVE;

    @Column(name = "visible_from_message_id")
    private Long visibleFromMessageId;

    // 목록 정렬/미리보기용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private ChatMessage lastMessage;

    // 목록 정렬용
    private LocalDateTime lastMessageSentAt;

    void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    /**
     * 채팅방을 나갈 때 호출
     */
    public void leftChatRoom(Long lastMessageIdOrNull) {
        this.participantState = ParticipantState.LEFT;
        this.visibleFromMessageId = lastMessageIdOrNull;
        // 목록에서 보이지 않게 lastMessage도 초기화
        this.lastMessage = null;
        this.lastMessageSentAt = null;
    }

    /**
     * 새 메시지가 전송될 때 호출
     */
    public void updateLastMessage(ChatMessage message) {
        this.participantState = ParticipantState.ACTIVE;
        this.lastMessage = message;
        this.lastMessageSentAt = message.getCreatedAt();
    }

}
