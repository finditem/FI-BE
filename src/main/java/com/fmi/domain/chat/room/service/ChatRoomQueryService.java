package com.fmi.domain.chat.room.service;

import static com.fmi.domain.chat.room.web.dto.response.ChatRoomResponse.*;

import com.fmi.domain.chat.room.converter.ChatRoomConverter;
import com.fmi.domain.chat.room.data.ChatRoom;
import com.fmi.domain.chat.room.data.ChatRoomParticipant;
import com.fmi.domain.chat.room.repository.ChatRoomParticipantRepository;
import com.fmi.domain.chat.room.repository.ChatRoomRepository;
import com.fmi.domain.post.findcompletion.data.FindCompletion;
import com.fmi.domain.post.findcompletion.repository.FindCompletionRepository;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.service.PostImageService;
import com.fmi.domain.post.review.repository.ReviewRepository;
import com.fmi.domain.user.block.service.BlockService;
import com.fmi.domain.user.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.enums.SortType;
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
public class ChatRoomQueryService {

    private final ChatRoomRepository chatRoomRepository;

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    private final PostImageService postImageService;

    private final BlockService blockService;

    private final FindCompletionRepository findCompletionRepository;

    private final ReviewRepository reviewRepository;

    public MyChatListResponse getMyPostChatRooms(
            User user, Long cursorId, int size, PostType type, String address, SortType sort) {

        PageRequest pageable = PageRequest.of(0, size);

        Slice<ChatRoomParticipant> participantSlice =
                chatRoomParticipantRepository.findMyChatRooms(user.getId(), cursorId, pageable, type, address, sort);

        return buildChatListResponse(participantSlice, user);
    }

    private MyChatListResponse buildChatListResponse(Slice<ChatRoomParticipant> participantSlice, User user) {
        List<ChatRoomParticipant> participants = participantSlice.getContent();

        List<Long> postIds = participants.stream()
                .filter(p -> !p.getChatRoom().isSourcePostDeleted()
                        && p.getChatRoom().getPost() != null)
                .map(p -> p.getChatRoom().getPost().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> thumbnailMap = postImageService.findThumbnailUrlMap(postIds);

        List<ChatRoomSummaryResponse> summaryDTOs =
                ChatRoomConverter.toChatRoomSummaryListDTO(participants, user, thumbnailMap);

        Long nextCursor = null;
        if (!participants.isEmpty() && participantSlice.hasNext()) {
            // 커서는 마지막 'ChatRoomParticipant'의 ID가 됨
            nextCursor = participants.get(participants.size() - 1).getId();
        }

        return new MyChatListResponse(summaryDTOs, nextCursor);
    }

    public ChatRoomResultResponse getChatRoomDetail(Long roomId, User user) {
        ChatRoom chatRoom = chatRoomRepository
                .findById(roomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));

        Long unreadCount = chatRoom.getParticipant(user.getId()).getUnreadCount();

        User opponent = chatRoom.getOtherParticipant(user.getId());

        boolean isBlocked = blockService.isBlocked(user.getId(), opponent.getId());

        FindCompletion completion = findFindCompletion(chatRoom);
        FindCompletionInfoResponse findCompletion = ChatRoomConverter.toFindCompletionInfoDTO(
                chatRoom,
                completion,
                canComplete(chatRoom, completion, user, opponent, isBlocked),
                canWriteReview(chatRoom, completion, user, opponent, isBlocked));

        // 게시글 완전 삭제 아닐 경우 -> fk로 최신 데이터
        if (!chatRoom.isSourcePostDeleted() && chatRoom.getPost() != null) {
            Post post = chatRoom.getPost();
            String thumbnailImageUrl = postImageService.findThumbnailImageUrl(post);
            return ChatRoomConverter.toChatRoomResultDTO(
                    chatRoom, opponent, post, unreadCount, thumbnailImageUrl, isBlocked, findCompletion);
        }

        // 게시글 완전 삭제 -> 스냅샷 사용
        return ChatRoomConverter.toChatRoomResultDTOFromSnapshot(
                chatRoom, opponent, unreadCount, isBlocked, findCompletion);
    }

    // 같은 게시글의 다른 채팅방에서 완료됐는지도 알아야 하므로 게시글 기준으로 조회
    private FindCompletion findFindCompletion(ChatRoom chatRoom) {
        Long postId = chatRoom.getPost() != null ? chatRoom.getPost().getId() : chatRoom.getSourcePostId();
        if (postId == null) {
            return null;
        }
        return findCompletionRepository.findBySourcePostId(postId).orElse(null);
    }

    // 찾기 완료 검증(FindCompletionValidator)과 같은 조건일 때만 찾기 완료 버튼을 노출
    private boolean canComplete(
            ChatRoom chatRoom, FindCompletion completion, User user, User opponent, boolean isBlocked) {
        Post post = chatRoom.getPost();
        return completion == null
                && post != null
                && !chatRoom.isSourcePostDeleted()
                && !post.isDeleted()
                && post.getPostType() == PostType.LOST
                && post.getPostStatus() == PostStatus.SEARCHING
                && post.getUser() != null
                && post.getUser().getId().equals(user.getId())
                && opponent.getDeletedAt() == null
                && !isBlocked;
    }

    // 이 채팅방에서 완료한 요청자가 아직 후기를 쓰지 않았을 때만 후기 작성 버튼을 노출
    // 상대가 탈퇴했거나 차단 관계이면 노출하지 않는다.
    private boolean canWriteReview(
            ChatRoom chatRoom, FindCompletion completion, User user, User opponent, boolean isBlocked) {
        return completion != null
                && completion.isCompletedIn(chatRoom.getId())
                && completion.isRequestedBy(user.getId())
                && opponent.getDeletedAt() == null
                && !isBlocked
                && !reviewRepository.existsByFindCompletionId(completion.getId());
    }
}
