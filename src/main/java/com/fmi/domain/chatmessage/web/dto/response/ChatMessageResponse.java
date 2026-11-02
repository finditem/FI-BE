package com.fmi.domain.chatmessage.web.dto.response;

import com.fmi.domain.chatmessage.data.enums.MessageType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatMessageResponse {

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MessageResponse {
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
    public static class MessageImageResponse {
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
    public static class MessageSliceResponse {
        private List<MessageItemResponse> messages;
        private Long nextCursor;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MessageItemResponse {
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
    public static class ListUpdateResponse {
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
    public static class ReadReceiptResponse {
        private Long roomId;
        private Long readerId;
        private Long lastReadMessageId;
    }
}
