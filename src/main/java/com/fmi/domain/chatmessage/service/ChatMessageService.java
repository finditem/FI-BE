package com.fmi.domain.chatmessage.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.chatmessage.converter.ChatMessageConverter;
import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatmessage.data.MessageImage;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.chatmessage.repository.ChatMessageRepository;
import com.fmi.domain.chatmessage.repository.MessageImageRepository;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.chatroom.service.ChatRoomPresenceService;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import com.fmi.domain.userblock.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fmi.domain.chatmessage.converter.ChatMessageConverter.messageImageResponseDTO;
import static com.fmi.domain.chatmessage.converter.ChatMessageConverter.messageResponseDTO;
import static com.fmi.domain.chatmessage.web.dto.ChatMessageRequestDTO.SendImageRequestDTO;
import static com.fmi.domain.chatmessage.web.dto.ChatMessageRequestDTO.SendMessageRequestDTO;
import static com.fmi.domain.chatmessage.web.dto.ChatMessageResponseDTO.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageService {

    private final SimpMessagingTemplate broker;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final MessageImageRepository messageImageRepository;
    private final S3Service s3Service;
    private final ChatRoomPresenceService presenceService;
    private final BlockService blockService;
    private final ChatNotificationService chatNotificationService;

    public MessageResponseDTO sendMessage(Long roomId, Long senderId, SendMessageRequestDTO req) {
        var room = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));
        var sender = userRepository.getReferenceById(senderId);

        if (!room.isParticipant(sender)) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        var recipient = room.getOtherParticipant(sender.getId());

        // 차단 여부 검사(양방향 중 하나라도 차단이면 전송 금지)
        if (blockService.isBlocked(sender.getId(), recipient.getId())) {
            throw new GeneralException(ErrorStatus._MESSAGE_NOT_ALLOWED);
        }

        var chatMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .user(sender)
                .messageType(MessageType.TEXT)
                .content(req.getContent())
                .createdAt(LocalDateTime.now())
                .build());

        updateParticipantsOnNewMessage(room.getId(), chatMessage);

        MessageResponseDTO responseDTO = messageResponseDTO(chatMessage);
        broker.convertAndSendToUser(recipient.getId().toString(), "/queue/messages", responseDTO);
        broker.convertAndSendToUser(sender.getId().toString(), "/queue/messages", responseDTO);
        return responseDTO;
    }

    public MessageImageResponseDTO sendImageMessage(Long roomId, SendImageRequestDTO requestDTO, Long userId) {

        List<MultipartFile> imageFiles = requestDTO.getImages();
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new GeneralException(ErrorStatus._IMAGE_NOT_PROVIDED);
        }

        User sender = userRepository.getReferenceById(userId);
        ChatRoom room = chatRoomRepository.getReferenceById(roomId);

        if (!room.isParticipant(sender)) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        User recipient = room.getOtherParticipant(sender.getId());

        if (blockService.isBlocked(sender.getId(), recipient.getId())) {
            throw new GeneralException(ErrorStatus._MESSAGE_NOT_ALLOWED);
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(room)
                .user(sender)
                .content(requestDTO.getContent())
                .messageType(MessageType.IMAGE)
                .createdAt(LocalDateTime.now())
                .build();

        List<String> imageUrls = s3Service.upload(imageFiles);
        imageUrls.forEach(url -> {
            MessageImage newImage = MessageImage.builder().imageUrl(url).build();
            chatMessage.addImage(newImage);
        });

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        updateParticipantsOnNewMessage(room.getId(), savedMessage);

        // DTO로 변환하여 WebSocket 브로드캐스팅
        MessageImageResponseDTO responseDTO = messageImageResponseDTO(savedMessage);
        broker.convertAndSendToUser(recipient.getId().toString(), "/queue/messages", responseDTO);
        broker.convertAndSendToUser(sender.getId().toString(), "/queue/messages", responseDTO);
        return responseDTO;
    }

    private void updateParticipantsOnNewMessage(Long roomId, ChatMessage newMessage) {
        List<ChatRoomParticipant> allParticipants = chatRoomParticipantRepository.findAllByChatRoom_Id(roomId);
        User sender = newMessage.getUser();
        Long senderId = sender.getId();

        for (ChatRoomParticipant pt : allParticipants) {
            // 메시지 갱신 및 ACTIVE 설정
            pt.updateLastMessage(newMessage);

            // 발신자가 보낸 메세지 읽음 처리
            if (pt.getUser().getId().equals(senderId)) {
                pt.readMessages(newMessage.getId());
            }

            // 수신자의 카운트만 증가
            if (!pt.getUser().getId().equals(senderId)) {
                boolean isPresent = presenceService.isUserPresent(roomId, pt.getUser().getId());
                if (isPresent) {
                    // 방안에 있음 -> 즉시 읽음. DB unreadCount = 0
                    pt.readMessages(newMessage.getId());
                    ReadReceiptDTO receiptDTO = ReadReceiptDTO.builder()
                            .roomId(roomId)
                            .readerId(pt.getUser().getId())
                            .lastReadMessageId(newMessage.getId())
                            .build();
                    broker.convertAndSendToUser(sender.getId().toString(), "/queue/read-receipts", receiptDTO);
                } else {
                    // 방 밖에 있음 -> 카운트 +1. DB unreadCount + 1
                    pt.onNewMessageArrived();
                    chatNotificationService.saveOrUpdateChatNotification(pt.getUser(), roomId, newMessage.getPreviewContent(), NotificationType.CHAT);
                }
            }
            // 갱신된 정보로 DTO 생성
            ListUpdateDTO listUpdateDTO = ListUpdateDTO.builder()
                    .roomId(roomId)
                    .lastMessage(newMessage.getPreviewContent())
                    .lastMessageSentAt(newMessage.getCreatedAt())
                    .unreadCount(pt.getUnreadCount())
                    .build();

            broker.convertAndSendToUser(pt.getUser().getId().toString(), "/queue/list-updates", listUpdateDTO);
        }
    }

    @Transactional(readOnly = true)
    public MessageSliceResponseDTO messageSlice(Long roomId, Long senderId, Long cursorId) {

        if (!chatRoomParticipantRepository.existsByUser_IdAndChatRoom_Id(senderId, roomId)) {
            throw new GeneralException(ErrorStatus._MESSAGE_NOT_ALLOWED);
        }

        ChatRoomParticipant me = chatRoomParticipantRepository.findByChatRoom_IdAndUser_Id(roomId, senderId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));

        Long cutoff = me.getVisibleFromMessageId();

        final int pageSize = 20;
        PageRequest pageable = PageRequest.of(0, pageSize);

        // 메인 메시지 조회 (이미지 조인 X)
        Slice<ChatMessage> messagesSlice;
        if (cursorId== null) {
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
        List<MessageDTO> messageDtos = finalMessages.stream()
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

        return MessageSliceResponseDTO.builder()
                .messages(messageDtos)
                .nextCursor(nextCursorId)
                .build();
    }

    public void readMessages(Long roomId, Long userId) {
        Long lastMessageId = chatMessageRepository.findTopByChatRoom_IdOrderByIdDesc(roomId)
                .map(ChatMessage::getId)
                .orElse(null);
        if (lastMessageId == null) {
            return;
        }

        ChatRoomParticipant chatRoomParticipant = chatRoomParticipantRepository.findByChatRoom_IdAndUser_Id(roomId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));

        chatRoomParticipant.readMessages(lastMessageId);

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));

        User recipient = chatRoom.getOtherParticipant(userId);

        ReadReceiptDTO receiptDTO = ReadReceiptDTO.builder()
                .roomId(roomId)
                .readerId(chatRoomParticipant.getUser().getId())
                .lastReadMessageId(lastMessageId)
                .build();

        broker.convertAndSendToUser(recipient.getId().toString(), "/queue/read-receipts", receiptDTO);

    }
}
