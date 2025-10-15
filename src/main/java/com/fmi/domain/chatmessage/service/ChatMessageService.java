package com.fmi.domain.chatmessage.service;

import com.fmi.domain.User;
import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatmessage.data.MessageImage;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.chatmessage.repositiory.ChatMessageRepository;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import com.fmi.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static com.fmi.domain.chatmessage.converter.ChatMessageConverter.messageImageResponseDTO;
import static com.fmi.domain.chatmessage.converter.ChatMessageConverter.messageResponseDTO;
import static com.fmi.domain.chatmessage.web.dto.ChatMessageRequestDTO.SendImageRequestDTO;
import static com.fmi.domain.chatmessage.web.dto.ChatMessageRequestDTO.SendMessageRequestDTO;
import static com.fmi.domain.chatmessage.web.dto.ChatMessageResponseDTO.MessageImageResponseDTO;
import static com.fmi.domain.chatmessage.web.dto.ChatMessageResponseDTO.MessageResponseDTO;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageService {

    private final SimpMessagingTemplate broker;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public MessageResponseDTO sendMessage(Long roomId, Long senderId, SendMessageRequestDTO req) {
        var room = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));
        var sender = userRepository.getReferenceById(senderId);
        var recipient = room.getOtherParticipant(sender.getId());

        var chatMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .user(sender)
                .messageType(MessageType.TEXT)
                .content(req.getContent())
                .createdAt(LocalDateTime.now())
                .build());

        room.updateLastMessage((req.getContent()));

        MessageResponseDTO responseDTO = messageResponseDTO(chatMessage);
        broker.convertAndSendToUser(recipient.getEmail(), "/queue/messages", responseDTO);
        broker.convertAndSendToUser(sender.getEmail(), "/queue/messages", responseDTO);
        return responseDTO;
    }

    public MessageImageResponseDTO sendImageMessage(Long roomId, SendImageRequestDTO requestDTO, Long userId) {

        List<MultipartFile> imageFiles = requestDTO.getImages();
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new GeneralException(ErrorStatus._IMAGE_NOT_PROVIDED);
        }

        User sender = userRepository.getReferenceById(userId);
        ChatRoom room = chatRoomRepository.getReferenceById(roomId);

        User recipient = room.getOtherParticipant(sender.getId());

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

        if (!StringUtils.hasText(savedMessage.getContent())) {
            room.updateLastMessage("사진을 보냈습니다.");
        }
        else {
            room.updateLastMessage(requestDTO.getContent());
        }

        // DTO로 변환하여 WebSocket 브로드캐스팅
        MessageImageResponseDTO responseDTO = messageImageResponseDTO(savedMessage);
        broker.convertAndSendToUser(recipient.getEmail(), "/queue/messages", responseDTO);
        broker.convertAndSendToUser(sender.getEmail(), "/queue/messages", responseDTO);
        return responseDTO;
    }
}
