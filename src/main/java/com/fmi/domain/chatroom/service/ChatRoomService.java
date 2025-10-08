package com.fmi.domain.chatroom.service;

import com.fmi.domain.User;
import com.fmi.domain.chatroom.converter.ChatRoomConverter;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public Pair<ChatRoomResultDTO, Boolean> createChatRoom(Long postId, Long contactUserId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        // 게시글 작성자의 ID와 채팅을 요청한 사용자의 ID가 같은지 확인
        if (post.getUser().getUserId().equals(contactUserId)) {
            // 같다면, 본인 글에 대한 채팅 시도이므로 예외 발생
            throw new GeneralException(ErrorStatus._CHATROOM_NOT_ALLOWED);
        }

        // 기존 채팅방이 있는지 조회
        Optional<ChatRoom> optionalChatRoom = chatRoomRepository.findByPost_PostIdAndUser_UserId(postId, contactUserId);

        User opponentUser = post.getUser();

        // 채팅방이 이미 존재하는 경우
        if (optionalChatRoom.isPresent()) {
            ChatRoom existingRoom = optionalChatRoom.get();
            ChatRoomResultDTO dto = ChatRoomConverter.toChatRoomResultDTO(existingRoom, opponentUser, post);
            return Pair.of(dto, false);
        }

        // 채팅방이 존재하지 않는 경우 (새로 생성)
        else {
            User user = userRepository.findById(contactUserId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

            ChatRoom newRoom = ChatRoom.builder()
                    .post(post)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();

            chatRoomRepository.save(newRoom);

            ChatRoomResultDTO dto = ChatRoomConverter.toChatRoomResultDTO(newRoom, opponentUser, post);
            return Pair.of(dto, true);
        }
    }

    public MyPostChatListDTO getMyPostChatRooms(Long postId, Long ownerId, Long cursorId, int size) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        if (!post.getUser().getUserId().equals(ownerId)) {
            throw new GeneralException(ErrorStatus._CHATROOM_FORBIDDEN);
        }

        Slice<ChatRoom> chatRoomSlice;

        // 커서 ID가 없으면 첫 페이지이므로, 간단한 쿼리 호출
        if (cursorId == null) {
            chatRoomSlice = chatRoomRepository.findMyPostChatRoomsFirstPage(
                    postId, PageRequest.of(0, size));
        } else {
            // 커서 ID가 있으면, 해당 ID의 updatedAt을 기준으로 다음 페이지를 조회
            ChatRoom cursorRoom = chatRoomRepository.findById(cursorId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus._INVALID_CURSOR));

            chatRoomSlice = chatRoomRepository.findMyPostChatRoomsWithCursor(
                    postId, cursorRoom.getUpdatedAt(), cursorId, PageRequest.of(0, size));
        }

        return buildChatListResponse(chatRoomSlice);
    }

    // DTO 변환 및 다음 커서 계산 메서드
    private MyPostChatListDTO buildChatListResponse(Slice<ChatRoom> chatRoomSlice) {
        List<ChatRoom> chatRooms = chatRoomSlice.getContent();

        List<ChatRoomSummaryDTO> summaryDTOs = ChatRoomConverter.toChatRoomSummaryListDTO(chatRooms);

        Long nextCursor = null;
        if (!chatRooms.isEmpty() && chatRoomSlice.hasNext()) {
            nextCursor = chatRooms.get(chatRooms.size() - 1).getChatroom_id();
        }

        return new MyPostChatListDTO(summaryDTOs, nextCursor);
    }
}