package com.fmi.domain.chat.message.service;

import static com.fmi.domain.chat.message.converter.ChatMessageConverter.messageImageResponseDTO;
import static com.fmi.domain.chat.message.converter.ChatMessageConverter.messageResponseDTO;
import static com.fmi.domain.chat.message.web.dto.request.ChatMessageRequest.SendMessageRequest;
import static com.fmi.domain.chat.message.web.dto.response.ChatMessageResponse.*;

import com.fmi.domain.chat.message.data.ChatMessage;
import com.fmi.domain.chat.message.data.MessageImage;
import com.fmi.domain.chat.message.data.enums.MessageType;
import com.fmi.domain.chat.message.repository.ChatMessageRepository;
import com.fmi.domain.chat.room.data.ChatRoom;
import com.fmi.domain.chat.room.data.ChatRoomParticipant;
import com.fmi.domain.chat.room.repository.ChatRoomParticipantRepository;
import com.fmi.domain.chat.room.repository.ChatRoomRepository;
import com.fmi.domain.chat.room.service.ChatRoomPresenceService;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.user.block.service.BlockService;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.external.storage.S3Service;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageService {

    private final SimpMessagingTemplate broker;

    private final ChatMessageRepository chatMessageRepository;

    private final ChatRoomRepository chatRoomRepository;

    private final UserRepository userRepository;

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    private final S3Service s3Service;

    private final ChatRoomPresenceService presenceService;

    private final BlockService blockService;

    private final ChatNotificationService chatNotificationService;

    public MessageResponse sendMessage(Long roomId, Long senderId, SendMessageRequest req) {
        var room = chatRoomRepository
                .findById(roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));
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

        MessageResponse responseDTO = messageResponseDTO(chatMessage);
        broker.convertAndSendToUser(recipient.getId().toString(), "/queue/messages", responseDTO);
        broker.convertAndSendToUser(sender.getId().toString(), "/queue/messages", responseDTO);
        return responseDTO;
    }

    public MessageImageResponse sendImageMessage(Long roomId, List<MultipartFile> images, Long userId) {

        if (images == null || images.isEmpty()) {
            throw new GeneralException(ErrorStatus._IMAGE_NOT_PROVIDED);
        }

        User sender = userRepository.getReferenceById(userId);
        ChatRoom room = chatRoomRepository.getReferenceById(roomId);

        if (!room.isParticipant(sender)) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        User recipient = room.getOtherParticipant(sender.getId());

        if (blockService.isBlocked(sender.getId(), recipient.getId())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(room)
                .user(sender)
                .messageType(MessageType.IMAGE)
                .createdAt(LocalDateTime.now())
                .build();

        List<String> imageUrls = s3Service.upload(images);
        imageUrls.forEach(url -> {
            MessageImage newImage = MessageImage.builder().imageUrl(url).build();
            chatMessage.addImage(newImage);
        });

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        updateParticipantsOnNewMessage(room.getId(), savedMessage);

        // DTO로 변환하여 WebSocket 브로드캐스팅
        MessageImageResponse responseDTO = messageImageResponseDTO(savedMessage);
        broker.convertAndSendToUser(recipient.getId().toString(), "/queue/messages", responseDTO);
        broker.convertAndSendToUser(sender.getId().toString(), "/queue/messages", responseDTO);
        return responseDTO;
    }

    private void updateParticipantsOnNewMessage(Long roomId, ChatMessage newMessage) {
        List<ChatRoomParticipant> allParticipants = chatRoomParticipantRepository.findAllByChatRoom_Id(roomId);
        User sender = newMessage.getUser();
        Long senderId = sender.getId();
        Long postId = newMessage.getChatRoom().isSourcePostDeleted()
                        || newMessage.getChatRoom().getPost() == null
                ? newMessage.getChatRoom().getSourcePostId()
                : newMessage.getChatRoom().getPost().getId();

        for (ChatRoomParticipant pt : allParticipants) {
            // 메시지 갱신 및 ACTIVE 설정
            pt.updateLastMessage(newMessage);

            // 발신자가 보낸 메세지 읽음 처리
            if (pt.getUser().getId().equals(senderId)) {
                pt.readMessages(newMessage.getId());
            }

            // 수신자의 카운트만 증가
            if (!pt.getUser().getId().equals(senderId)) {
                boolean isPresent =
                        presenceService.isUserPresent(roomId, pt.getUser().getId());
                if (isPresent) {
                    // 방안에 있음 -> 즉시 읽음. DB unreadCount = 0
                    pt.readMessages(newMessage.getId());
                    ReadReceiptResponse receiptDTO = ReadReceiptResponse.builder()
                            .roomId(roomId)
                            .readerId(pt.getUser().getId())
                            .lastReadMessageId(newMessage.getId())
                            .build();
                    broker.convertAndSendToUser(sender.getId().toString(), "/queue/read-receipts", receiptDTO);
                } else {
                    // 방 밖에 있음 -> 카운트 +1. DB unreadCount + 1
                    pt.onNewMessageArrived();
                    chatNotificationService.saveOrUpdateChatNotification(
                            pt.getUser(), postId, roomId, newMessage.getContent(), NotificationType.CHAT);
                }
            }
            // 갱신된 정보로 DTO 생성
            ListUpdateResponse listUpdateDTO = ListUpdateResponse.builder()
                    .roomId(roomId)
                    .messageType(newMessage.getMessageType())
                    .lastMessage(newMessage.getContent())
                    .lastMessageSentAt(newMessage.getCreatedAt())
                    .unreadCount(pt.getUnreadCount())
                    .build();

            broker.convertAndSendToUser(pt.getUser().getId().toString(), "/queue/list-updates", listUpdateDTO);
        }
    }

    public void readMessages(Long roomId, Long userId) {
        Long lastMessageId = chatMessageRepository
                .findTopByChatRoom_IdOrderByIdDesc(roomId)
                .map(ChatMessage::getId)
                .orElse(null);
        if (lastMessageId == null) {
            return;
        }

        ChatRoomParticipant chatRoomParticipant = chatRoomParticipantRepository
                .findByChatRoom_IdAndUser_Id(roomId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PARTICIPANT_NOT_FOUND));

        chatRoomParticipant.readMessages(lastMessageId);

        ChatRoom chatRoom = chatRoomRepository
                .findById(roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));

        User recipient = chatRoom.getOtherParticipant(userId);

        ReadReceiptResponse receiptDTO = ReadReceiptResponse.builder()
                .roomId(roomId)
                .readerId(chatRoomParticipant.getUser().getId())
                .lastReadMessageId(lastMessageId)
                .build();

        broker.convertAndSendToUser(recipient.getId().toString(), "/queue/read-receipts", receiptDTO);
    }
}
