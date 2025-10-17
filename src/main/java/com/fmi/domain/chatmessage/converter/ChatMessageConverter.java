package com.fmi.domain.chatmessage.converter;

import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatmessage.data.MessageImage;

import java.util.stream.Collectors;

import static com.fmi.domain.chatmessage.web.dto.ChatMessageResponseDTO.MessageImageResponseDTO;
import static com.fmi.domain.chatmessage.web.dto.ChatMessageResponseDTO.MessageResponseDTO;

public class ChatMessageConverter {

    public static MessageResponseDTO messageResponseDTO(ChatMessage chatMessage) {
        return MessageResponseDTO.builder()
                .messageId(chatMessage.getId())
                .messageType(chatMessage.getMessageType())
                .content(chatMessage.getContent())
                .roomId(chatMessage.getChatRoom().getId())
                .senderId(chatMessage.getUser().getId())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }

    public static MessageImageResponseDTO messageImageResponseDTO(ChatMessage chatMessage) {
        return MessageImageResponseDTO.builder()
                .messageId(chatMessage.getId())
                .messageType(chatMessage.getMessageType())
                .roomId(chatMessage.getChatRoom().getId())
                .senderId(chatMessage.getUser().getId())
                .content(chatMessage.getContent())
                .imageUrls(chatMessage.getImages().stream()
                        .map(MessageImage::getImageUrl)
                        .collect(Collectors.toList()))
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }

}
