package com.fmi.domain.chatroom.service;

import com.fmi.domain.Enum.SortType;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatmessage.repository.ChatMessageRepository;
import com.fmi.domain.chatroom.converter.ChatRoomConverter;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.data.enums.ParticipantState;
import com.fmi.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.PostImageService;
import com.fmi.domain.userblock.service.BlockService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.*;

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

    public Pair<ChatRoomResultDTO, Boolean> createChatRoom(Long postId, Long contactUserId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new GeneralException(ErrorStatus._POST_NOT_FOUND);
        }

        // 게시글 작성자의 ID와 채팅을 요청한 사용자의 ID가 같은지 확인
        if (post.getUser().getId().equals(contactUserId)) {
            // 같다면, 본인 글에 대한 채팅 시도이므로 예외 발생
            throw new GeneralException(ErrorStatus._CHATROOM_NOT_ALLOWED);
        }

        // 기존 채팅방이 있는지 조회
        Optional<ChatRoom> optionalChatRoom = chatRoomRepository.findChatRoomByPostAndUsers(postId, post.getUser().getId(), contactUserId);

        User opponentUser = post.getUser();

        String thumbnailImageUrl = postImageService.findThumbnailImageUrl(post);

        boolean isBlocked = blockService.isBlocked(contactUserId, opponentUser.getId());

        // 채팅방이 이미 존재하는 경우
        if (optionalChatRoom.isPresent()) {
            ChatRoom existingRoom = optionalChatRoom.get();

            Long unreadCount = existingRoom.getParticipant(contactUserId).getUnreadCount();

            ChatRoomResultDTO dto = ChatRoomConverter.toChatRoomResultDTO(existingRoom, opponentUser, post, unreadCount, thumbnailImageUrl, isBlocked);

            return Pair.of(dto, false);
        }

        // 채팅방이 존재하지 않는 경우 (새로 생성)
        else {
            User user = userRepository.findActiveById(contactUserId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
            User postAuthor = post.getUser();
            ChatRoom newRoom = ChatRoom.builder()
                    .post(post)
                    .createdAt(LocalDateTime.now())
                    .build();
            newRoom.initFromPost(post);

            // 두 명의 참여자(게시글 작성자, 연락한 사람)에 대한 ChatRoomParticipant 엔티티를 생성
            ChatRoomParticipant authorParticipant = ChatRoomParticipant.builder()
                    .user(postAuthor)
                    .build();

            ChatRoomParticipant contactUserParticipant = ChatRoomParticipant.builder()
                    .user(user)
                    .build();

            newRoom.addParticipant(authorParticipant);
            newRoom.addParticipant(contactUserParticipant);

            chatRoomRepository.save(newRoom);

            ChatRoomResultDTO dto = ChatRoomConverter.toChatRoomResultDTO(newRoom, opponentUser, post, 0L, thumbnailImageUrl, isBlocked);

            return Pair.of(dto, true);
        }
    }

    public MyChatListDTO getMyPostChatRooms(User user, Long cursorId, int size, PostType type, String address, SortType sort) {

        PageRequest pageable = PageRequest.of(0, size);

        Slice<ChatRoomParticipant> participantSlice = chatRoomParticipantRepository.findMyChatRooms(
                user.getId(),
                cursorId,
                pageable,
                type, address, sort
        );

        return buildChatListResponse(participantSlice, user);
    }

    private MyChatListDTO buildChatListResponse(Slice<ChatRoomParticipant> participantSlice, User user) {
        List<ChatRoomParticipant> participants = participantSlice.getContent();

        List<Long> postIds = participants.stream()
                .filter(p -> !p.getChatRoom().isSourcePostDeleted())
                .map(p -> p.getChatRoom().getPost().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> thumbnailMap = postImageService.findThumbnailUrlMap(postIds);

        List<ChatRoomSummaryDTO> summaryDTOs = ChatRoomConverter.toChatRoomSummaryListDTO(participants, user, thumbnailMap);

        Long nextCursor = null;
        if (!participants.isEmpty() && participantSlice.hasNext()) {
            // 커서는 마지막 'ChatRoomParticipant'의 ID가 됨
            nextCursor = participants.get(participants.size() - 1).getId();
        }

        return new MyChatListDTO(summaryDTOs, nextCursor);
    }

    public void leaveChatRoom(Long roomId, Long userId) {
        ChatRoomParticipant chatRoomParticipant = chatRoomParticipantRepository.findByUser_IdAndChatRoom_Id(userId, roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PARTICIPANT_NOT_FOUND));

        if (chatRoomParticipant.getParticipantState() == ParticipantState.LEFT) {
            return;
        }

        Long lastMsgId = chatMessageRepository.findTopByChatRoom_IdOrderByIdDesc(roomId)
                .map(ChatMessage::getId).orElse(null);

        chatRoomParticipant.leftChatRoom(lastMsgId);
    }

    public ChatRoomResultDTO getChatRoomDetail(Long roomId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));

        Long unreadCount = chatRoom.getParticipant(user.getId()).getUnreadCount();

        User opponent = chatRoom.getOtherParticipant(user.getId());

        boolean isBlocked = blockService.isBlocked(user.getId(), opponent.getId());

        // 게시글 완전 삭제 아닐 경우 -> fk로 최신 데이터
        if(!chatRoom.isSourcePostDeleted()) {
            Post post = chatRoom.getPost();
            String thumbnailImageUrl = postImageService.findThumbnailImageUrl(post);
            return ChatRoomConverter.toChatRoomResultDTO(chatRoom, opponent, post, unreadCount, thumbnailImageUrl, isBlocked);
        }

        // 게시글 완전 삭제 -> 스냅샷 사용
        return ChatRoomConverter.toChatRoomResultDTOFromSnapshot(chatRoom, opponent, unreadCount, isBlocked);

    }

}
