package com.fmi.domain.chatmessage.web.dto;

import com.fmi.domain.chatmessage.data.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
        private String content;
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

}
