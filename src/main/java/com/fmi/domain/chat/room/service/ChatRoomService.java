package com.fmi.domain.chat.room.service;

import static com.fmi.domain.chat.room.web.dto.response.ChatRoomResponse.*;

import com.fmi.domain.chat.message.data.ChatMessage;
import com.fmi.domain.chat.message.repository.ChatMessageRepository;
import com.fmi.domain.chat.room.converter.ChatRoomConverter;
import com.fmi.domain.chat.room.data.ChatRoom;
import com.fmi.domain.chat.room.data.ChatRoomParticipant;
import com.fmi.domain.chat.room.data.enums.ParticipantState;
import com.fmi.domain.chat.room.repository.ChatRoomParticipantRepository;
import com.fmi.domain.chat.room.repository.ChatRoomRepository;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.post.post.service.PostImageService;
import com.fmi.domain.user.block.service.BlockService;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    private final PostRepository postRepository;

    private final UserRepository userRepository;

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    private final ChatMessageRepository chatMessageRepository;

    private final PostImageService postImageService;

    private final BlockService blockService;

    public Pair<ChatRoomResultResponse, Boolean> createChatRoom(Long postId, Long contactUserId) {

        Post post =
                postRepository.findById(postId).orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new GeneralException(ErrorStatus._POST_NOT_FOUND);
        }

        // 게시글 작성자의 ID와 채팅을 요청한 사용자의 ID가 같은지 확인
        if (post.getUser().getId().equals(contactUserId)) {
            // 같다면, 본인 글에 대한 채팅 시도이므로 예외 발생
            throw new GeneralException(ErrorStatus._CHATROOM_NOT_ALLOWED);
        }

        // 기존 채팅방이 있는지 조회
        Optional<ChatRoom> optionalChatRoom = chatRoomRepository.findChatRoomByPostAndUsers(
                postId, post.getUser().getId(), contactUserId);

        User opponentUser = post.getUser();

        String thumbnailImageUrl = postImageService.findThumbnailImageUrl(post);

        boolean isBlocked = blockService.isBlocked(contactUserId, opponentUser.getId());

        // 채팅방이 이미 존재하는 경우
        if (optionalChatRoom.isPresent()) {
            ChatRoom existingRoom = optionalChatRoom.get();

            Long unreadCount = existingRoom.getParticipant(contactUserId).getUnreadCount();

            ChatRoomResultResponse dto = ChatRoomConverter.toChatRoomResultDTO(
                    existingRoom, opponentUser, post, unreadCount, thumbnailImageUrl, isBlocked);

            return Pair.of(dto, false);
        }

        // 채팅방이 존재하지 않는 경우 (새로 생성)
        else {
            User user = userRepository
                    .findActiveById(contactUserId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
            User postAuthor = post.getUser();
            ChatRoom newRoom =
                    ChatRoom.builder().post(post).createdAt(LocalDateTime.now()).build();
            newRoom.initFromPost(post);

            // 두 명의 참여자(게시글 작성자, 연락한 사람)에 대한 ChatRoomParticipant 엔티티를 생성
            ChatRoomParticipant authorParticipant =
                    ChatRoomParticipant.builder().user(postAuthor).build();

            ChatRoomParticipant contactUserParticipant =
                    ChatRoomParticipant.builder().user(user).build();

            newRoom.addParticipant(authorParticipant);
            newRoom.addParticipant(contactUserParticipant);

            chatRoomRepository.save(newRoom);

            ChatRoomResultResponse dto = ChatRoomConverter.toChatRoomResultDTO(
                    newRoom, opponentUser, post, 0L, thumbnailImageUrl, isBlocked);

            return Pair.of(dto, true);
        }
    }

    public void leaveChatRoom(Long roomId, Long userId) {
        ChatRoomParticipant chatRoomParticipant = chatRoomParticipantRepository
                .findByUser_IdAndChatRoom_Id(userId, roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PARTICIPANT_NOT_FOUND));

        if (chatRoomParticipant.getParticipantState() == ParticipantState.LEFT) {
            return;
        }

        Long lastMsgId = chatMessageRepository
                .findTopByChatRoom_IdOrderByIdDesc(roomId)
                .map(ChatMessage::getId)
                .orElse(null);

        chatRoomParticipant.leftChatRoom(lastMsgId);
    }
}
