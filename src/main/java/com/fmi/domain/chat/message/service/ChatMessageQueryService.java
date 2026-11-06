package com.fmi.domain.chat.message.service;

import static com.fmi.domain.chat.message.web.dto.response.ChatMessageResponse.*;

import com.fmi.domain.chat.message.converter.ChatMessageConverter;
import com.fmi.domain.chat.message.data.ChatMessage;
import com.fmi.domain.chat.message.data.MessageImage;
import com.fmi.domain.chat.message.data.enums.MessageType;
import com.fmi.domain.chat.message.repository.ChatMessageRepository;
import com.fmi.domain.chat.message.repository.MessageImageRepository;
import com.fmi.domain.chat.room.data.ChatRoomParticipant;
import com.fmi.domain.chat.room.repository.ChatRoomParticipantRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageQueryService {

    private final ChatMessageRepository chatMessageRepository;

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    private final MessageImageRepository messageImageRepository;

    @Transactional(readOnly = true)
    public MessageSliceResponse messageSlice(Long roomId, Long senderId, Long cursorId) {
        ChatRoomParticipant me = chatRoomParticipantRepository
                .findByChatRoom_IdAndUser_Id(roomId, senderId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._MESSAGE_NOT_ALLOWED));

        Long cutoff = me.getVisibleFromMessageId();

        final int pageSize = 20;
        PageRequest pageable = PageRequest.of(0, pageSize);

        // 메인 메시지 조회 (이미지 조인 X)
        Slice<ChatMessage> messagesSlice;
        if (cursorId == null) {
            messagesSlice = chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, cutoff, pageable);
        } else {
            messagesSlice = chatMessageRepository.findByRoomIdWithCursor(roomId, cursorId, cutoff, pageable);
        }

        List<ChatMessage> finalMessages = messagesSlice.getContent();

        // 메시지 ID 목록 추출
        List<Long> imageMessageIds = finalMessages.stream()
                .filter(m -> m.getMessageType() == MessageType.IMAGE)
                .map(ChatMessage::getId)
                .toList();

        // 이미지 메시지가 있을 경우에만, 이미지 목록을 IN 쿼리로 한방에 조회
        Map<Long, List<String>> imagesMap = Collections.emptyMap();
        if (!imageMessageIds.isEmpty()) {
            List<MessageImage> images = messageImageRepository.findAllByChatMessage_IdIn(imageMessageIds);

            // (메시지 ID)별로 (이미지 URL 리스트)를 그룹핑
            imagesMap = images.stream()
                    .collect(Collectors.groupingBy(
                            image -> image.getChatMessage().getId(), // 부모 메시지 ID로 그룹핑
                            Collectors.mapping(MessageImage::getImageUrl, Collectors.toList()) // URL을 List로
                            ));
        }

        // DTO로 변환
        Map<Long, List<String>> finalImagesMap = imagesMap;
        List<MessageItemResponse> messageDtos = finalMessages.stream()
                .map(message -> {
                    // 맵에서 해당 메시지의 이미지 리스트를 찾음 (없으면 빈 리스트)
                    List<String> imageUrls = finalImagesMap.getOrDefault(message.getId(), Collections.emptyList());
                    return ChatMessageConverter.GetMessageResponseDTO(message, imageUrls);
                })
                .toList();

        Long nextCursorId = null;
        // 다음 페이지가 있고, 현재 조회된 메시지가 실제로 있을 때
        if (messagesSlice.hasNext() && !finalMessages.isEmpty()) {
            nextCursorId = finalMessages.get(finalMessages.size() - 1).getId();
        }

        return MessageSliceResponse.builder()
                .messages(messageDtos)
                .nextCursor(nextCursorId)
                .build();
    }
}
