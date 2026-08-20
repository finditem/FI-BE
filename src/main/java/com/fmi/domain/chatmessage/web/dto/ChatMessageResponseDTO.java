package com.fmi.domain.chatmessage.web.dto;

import com.fmi.domain.chatmessage.data.enums.MessageType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatMessageResponseDTO {

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MessageResponseDTO {
        private Long messageId;
        private Long roomId;
        private Long senderId;
        private String content;
        private MessageType messageType;
        private LocalDateTime createdAt;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MessageImageResponseDTO {
        private Long messageId;
        private Long roomId;
        private Long senderId;
        private MessageType messageType;
        private List<String> imageUrls;
        private LocalDateTime createdAt;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MessageSliceResponseDTO {
        private List<MessageDTO> messages;
        private Long nextCursor;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MessageDTO {
        private Long messageId;
        private String messageType;
        private Long senderId;
        private String content;
        private List<String> imageUrls;
        private LocalDateTime createdAt;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ListUpdateDTO {
        private Long roomId;
        private MessageType messageType;
        private String lastMessage;
        private LocalDateTime lastMessageSentAt;
        private Long unreadCount; // DB에 저장된 값
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ReadReceiptDTO {
        private Long roomId;
        private Long readerId;
        private Long lastReadMessageId;
    }
}
