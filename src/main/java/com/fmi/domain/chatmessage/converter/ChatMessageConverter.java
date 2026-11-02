package com.fmi.domain.chatmessage.converter;

import static com.fmi.domain.chatmessage.web.dto.response.ChatMessageResponse.MessageImageResponse;
import static com.fmi.domain.chatmessage.web.dto.response.ChatMessageResponse.MessageResponse;

import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatmessage.data.MessageImage;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.chatmessage.web.dto.response.ChatMessageResponse;
import java.util.List;
import java.util.stream.Collectors;

public class ChatMessageConverter {

    public static MessageResponse messageResponseDTO(ChatMessage chatMessage) {
        return MessageResponse.builder()
                .messageId(chatMessage.getId())
                .messageType(chatMessage.getMessageType())
                .content(chatMessage.getContent())
                .roomId(chatMessage.getChatRoom().getId())
                .senderId(chatMessage.getUser().getId())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }

    public static MessageImageResponse messageImageResponseDTO(ChatMessage chatMessage) {
        return MessageImageResponse.builder()
                .messageId(chatMessage.getId())
                .messageType(chatMessage.getMessageType())
                .roomId(chatMessage.getChatRoom().getId())
                .senderId(chatMessage.getUser().getId())
                .imageUrls(chatMessage.getImages().stream()
                        .map(MessageImage::getImageUrl)
                        .collect(Collectors.toList()))
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }

    public static ChatMessageResponse.MessageItemResponse GetMessageResponseDTO(
            ChatMessage message, List<String> imageUrls) {

        return ChatMessageResponse.MessageItemResponse.builder()
                .messageId(message.getId())
                .messageType(message.getMessageType().name())
                .senderId(message.getUser().getId())
                .content(message.getContent() == null || message.getContent().isEmpty() ? null : message.getContent())
                .imageUrls(message.getMessageType() == MessageType.IMAGE ? imageUrls : null)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
