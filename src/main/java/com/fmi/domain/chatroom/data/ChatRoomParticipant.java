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

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "unread_count")
    @Builder.Default
    private Long unreadCount = 0L;

    // 최초로 안 읽은 메세지 도착 시간
    @Column(name = "first_unread_at")
    private LocalDateTime firstUnreadAt;

    // 리마인더 발송 여부
    @Column(name = "last_reminded_at")
    private LocalDateTime lastRemindedAt;

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

    /**
     * 새 메시지가 도착했을 때 호출
     */
    public void onNewMessageArrived(ChatMessage message) {
        this.participantState = ParticipantState.ACTIVE;
        this.lastMessage = message;
        this.lastMessageSentAt = message.getCreatedAt();

        // 처음으로 안 읽은 시간 기록
        if (this.unreadCount == 0L) {
            this.firstUnreadAt = LocalDateTime.now();
        }

        this.unreadCount++;
    }

    /**
     * 메시지 읽음 처리 (카운트 0으로 초기화)
     */
    public void readMessages(Long lastReadId) {
        if (this.lastReadMessageId == null || lastReadId > this.lastReadMessageId) {
            this.lastReadMessageId = lastReadId;
        }
        this.unreadCount = 0L;

        this.firstUnreadAt = null;
        this.lastRemindedAt = null;
    }

    /**
     * 안 읽은 개수 1 증가
     */
    public void incrementUnreadCount() {
        if (this.unreadCount == null) {
            this.unreadCount = 0L;
        }
        this.unreadCount += 1;
    }

    /**
     * 리마인더 발송 후 호출
     */
    public void markReminded() {
        this.lastRemindedAt = LocalDateTime.now();
    }
}
