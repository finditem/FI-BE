package com.fmi.domain.chatmessage.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.chatmessage.repository.ChatMessageRepository;
import com.fmi.domain.chatmessage.web.dto.ChatMessageRequestDTO;
import com.fmi.domain.chatmessage.web.dto.ChatMessageResponseDTO;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.chatroom.service.ChatRoomPresenceService;
import com.fmi.domain.userblock.service.BlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMessageServiceTest {

    @Mock
    private SimpMessagingTemplate broker;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;
    @Mock
    private ChatRoomPresenceService presenceService;
    @Mock
    private BlockService blockService;
    @Mock
    private ChatNotificationService chatNotificationService;
    @Mock
    private ChatRoom mockRoom;;

    private User sender;
    private User recipient;
    private ChatMessage newMessage;
    private ChatRoomParticipant recipientPt;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L)
                .email("sender@gmail.com")
                .build();
        recipient = User.builder()
                .id(2L)
                .email("recipient@gmail.com")
                .build();

        newMessage = ChatMessage.builder()
                .id(100L)
                .user(sender)
                .content("텍스트 메시지")
                .messageType(MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .chatRoom(mockRoom)
                .build();

        recipientPt = ChatRoomParticipant.builder()
                .user(recipient)
                .unreadCount(0L)
                .build();
        given(userRepository.getReferenceById(1L)).willReturn(sender);
    }

    @Test
    @DisplayName("방 밖에 있는 유저에게 메세지 전송 시, unreadCount +1 및 list-update 전송")
    void testSendMessage_When_Recipient_Is_Absent() {

        // given
        given(presenceService.isUserPresent(anyLong(), eq(2L))).willReturn(false);

        given(chatRoomParticipantRepository.findAllByChatRoom_Id(anyLong())).willReturn(List.of(recipientPt));

        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(newMessage);

        given(chatRoomRepository.findById(anyLong())).willReturn(Optional.of(mockRoom));

        given(mockRoom.isParticipant(sender)).willReturn(true);
        given(mockRoom.getOtherParticipant(1L)).willReturn(recipient);

        // when
        chatMessageService.sendMessage(123L, 1L, new ChatMessageRequestDTO.SendMessageRequestDTO("테스트 메세지"));

        // then
        assertThat(recipientPt.getUnreadCount()).isEqualTo(1);

        then(broker).should(times(1)).convertAndSendToUser(
                eq("2"),
                eq("/queue/list-updates"),
                any(ChatMessageResponseDTO.ListUpdateDTO.class)
        );

    }

    @Test
    @DisplayName("방 안에 있는 유저에게 메시지 전송 시, unreadCount 0 유지 및 read-receipt 전송")
    void testSendMessage_When_Recipient_Is_Present() {
        // given
        given(presenceService.isUserPresent(anyLong(), eq(2L)))
                .willReturn(true);

        given(chatRoomParticipantRepository.findAllByChatRoom_Id(anyLong()))
                .willReturn(List.of(recipientPt));

        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(newMessage);

        given(chatRoomRepository.findById(anyLong())).willReturn(Optional.of(mockRoom));

        given(mockRoom.isParticipant(sender)).willReturn(true);

        given(mockRoom.getOtherParticipant(1L)).willReturn(recipient);

        // when
        chatMessageService.sendMessage(123L, 1L, new ChatMessageRequestDTO.SendMessageRequestDTO("테스트 메시지"));

        // then
        // unreadCount가 0으로 유지되었는지 확인
        assertThat(recipientPt.getUnreadCount()).isEqualTo(0L);

        // read-receipt이 전송되었는지 확인
        then(broker).should(times(1)).convertAndSendToUser(
                eq("1"), // 메세지 보낸 사람에게
                eq("/queue/read-receipts"), // 읽음 전송
                any(ChatMessageResponseDTO.ReadReceiptDTO.class)
        );

        then(broker).should(times(1)).convertAndSendToUser(
                eq("2"),
                eq("/queue/list-updates"),
                any(ChatMessageResponseDTO.ListUpdateDTO.class)
        );

    }

    @Test
    @DisplayName("readMessages 호출 시, unreadCount가 3에서 0으로 바뀌어야 한다")
    void testReadMessages() {

        // given
        Long roomId = 123L;
        Long userId = 2L;

        ChatRoomParticipant recipientPt2 = ChatRoomParticipant.builder()
                .user(recipient)
                .unreadCount(3L)
                .build();

        given(chatMessageRepository.findTopByChatRoom_IdOrderByIdDesc(roomId))
                .willReturn(Optional.of(newMessage));

        given(chatRoomParticipantRepository.findByChatRoom_IdAndUser_Id(roomId, userId))
                .willReturn(Optional.of(recipientPt2));

        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockRoom));
        given(mockRoom.getOtherParticipant(userId)).willReturn(sender);

        // when
        chatMessageService.readMessages(roomId, userId);

        // then
        assertThat(recipientPt2.getUnreadCount()).isEqualTo(0L);
        assertThat(recipientPt2.getLastReadMessageId()).isEqualTo(100L);

    }
}