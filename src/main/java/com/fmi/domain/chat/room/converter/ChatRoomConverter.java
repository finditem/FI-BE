package com.fmi.domain.chat.room.converter;

import static com.fmi.domain.chat.room.web.dto.response.ChatRoomResponse.*;

import com.fmi.domain.chat.message.data.ChatMessage;
import com.fmi.domain.chat.message.data.enums.MessageType;
import com.fmi.domain.chat.room.data.ChatRoom;
import com.fmi.domain.chat.room.data.ChatRoomParticipant;
import com.fmi.domain.post.findcompletion.data.FindCompletion;
import com.fmi.domain.post.findcompletion.data.enums.FindCompletionStatus;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.user.user.data.User;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatRoomConverter {

    public static ChatRoomResultResponse toChatRoomResultDTO(
            ChatRoom chatRoom, User user, Post post, Long unreadCount, String thumbnailUrl, boolean isBlocked) {
        return toChatRoomResultDTO(chatRoom, user, post, unreadCount, thumbnailUrl, isBlocked, null);
    }

    public static ChatRoomResultResponse toChatRoomResultDTO(
            ChatRoom chatRoom,
            User user,
            Post post,
            Long unreadCount,
            String thumbnailUrl,
            boolean isBlocked,
            FindCompletionInfoResponse findCompletion) {

        var opponentUser = buildOpponentUserDTO(user, isBlocked);

        var postInfo = PostInfoResponse.builder()
                .postId(post.getId())
                .postType(post.getPostType())
                .category(post.getCategory())
                .title(post.getTitle())
                .address(post.getAddress())
                .thumbnailUrl(thumbnailUrl)
                .postStatus(post.getPostStatus())
                .deleted(post.isDeleted())
                .build();

        return ChatRoomResultResponse.builder()
                .roomId(chatRoom.getId())
                .unreadCount(unreadCount)
                .opponentUser(opponentUser)
                .postInfo(postInfo)
                .findCompletion(findCompletion)
                .build();
    }

    public static ChatRoomResultResponse toChatRoomResultDTOFromSnapshot(
            ChatRoom chatRoom,
            User opponent,
            Long unreadCount,
            boolean isBlocked,
            FindCompletionInfoResponse findCompletion) {

        var opponentUser = buildOpponentUserDTO(opponent, isBlocked);

        var postInfo = PostInfoResponse.builder()
                .postId(chatRoom.getSourcePostId())
                .postType(chatRoom.getSourcePostType())
                .category(chatRoom.getSourceCategory())
                .title(chatRoom.getSourceTitle())
                .address(chatRoom.getSourceAddress())
                .thumbnailUrl(chatRoom.getSourceThumbnailUrl())
                .postStatus(chatRoom.getSourcePostStatus())
                .deleted(true)
                .build();

        return ChatRoomResultResponse.builder()
                .roomId(chatRoom.getId())
                .unreadCount(unreadCount)
                .opponentUser(opponentUser)
                .postInfo(postInfo)
                .findCompletion(findCompletion)
                .build();
    }

    public static FindCompletionInfoResponse toFindCompletionInfoDTO(
            ChatRoom chatRoom, FindCompletion findCompletion, boolean canComplete, boolean canWriteReview) {
        if (findCompletion == null) {
            return FindCompletionInfoResponse.builder()
                    .status(FindCompletionStatus.NONE)
                    .canComplete(canComplete)
                    .build();
        }

        if (findCompletion.isCompletedIn(chatRoom.getId())) {
            return FindCompletionInfoResponse.builder()
                    .status(FindCompletionStatus.COMPLETED_HERE)
                    .findCompletionId(findCompletion.getId())
                    .canWriteReview(canWriteReview)
                    .build();
        }

        return FindCompletionInfoResponse.builder()
                .status(FindCompletionStatus.COMPLETED_ELSEWHERE)
                .build();
    }

    public static List<ChatRoomSummaryResponse> toChatRoomSummaryListDTO(
            List<ChatRoomParticipant> participants, User currentUser, Map<Long, String> thumbnailMap) {
        return participants.stream()
                .map(pt -> {
                    User contactUser = pt.getChatRoom().getOtherParticipant(currentUser.getId());

                    return toChatRoomSummaryDTO(pt, contactUser, thumbnailMap);
                })
                .collect(Collectors.toList());
    }

    public static ChatRoomSummaryResponse toChatRoomSummaryDTO(
            ChatRoomParticipant participant, User contactUser, Map<Long, String> thumbnailMap) {

        ContactUserResponse contactUserDTO = buildContactUserDTO(contactUser);

        ChatRoom room = participant.getChatRoom();

        PostInfoResponse postInfoDTO;
        if (!room.isSourcePostDeleted() && room.getPost() != null) {
            Post post = room.getPost();
            postInfoDTO = PostInfoResponse.builder()
                    .postId(post.getId())
                    .postType(post.getPostType())
                    .category(post.getCategory())
                    .title(post.getTitle())
                    .address(post.getAddress())
                    .thumbnailUrl(thumbnailMap.get(post.getId()))
                    .postStatus(post.getPostStatus())
                    .deleted(false)
                    .build();
        } else {
            postInfoDTO = PostInfoResponse.builder()
                    .postId(room.getSourcePostId())
                    .postType(room.getSourcePostType())
                    .category(room.getSourceCategory())
                    .title(room.getSourceTitle())
                    .address(room.getSourceAddress())
                    .thumbnailUrl(room.getSourceThumbnailUrl())
                    .postStatus(room.getSourcePostStatus())
                    .deleted(true)
                    .build();
        }

        ChatMessage lastMessage = participant.getLastMessage();
        String content = null;
        MessageType messageType = null;

        if (lastMessage != null) {
            content = lastMessage.getContent();
            messageType = lastMessage.getMessageType();
        }

        return ChatRoomSummaryResponse.builder()
                .roomId(participant.getChatRoom().getId())
                .contactUser(contactUserDTO)
                .postInfo(postInfoDTO)
                .messageType(messageType)
                .lastMessage(content)
                .lastMessageSentAt(participant.getLastMessageSentAt())
                .unreadCount(participant.getUnreadCount())
                .build();
    }

    private static boolean isImageMessage(ChatMessage message) {
        return message.getMessageType() == MessageType.IMAGE;
    }

    private static ContactUserResponse buildContactUserDTO(User user) {
        boolean withdrawn = user.getDeletedAt() != null;

        if (withdrawn) {
            return ContactUserResponse.builder()
                    .userId(user.getId())
                    .nickname(null)
                    .profileImageUrl(null)
                    .withdrawn(true)
                    .build();
        }

        return ContactUserResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfile_img())
                .withdrawn(false)
                .build();
    }

    private static OpponentUserResponse buildOpponentUserDTO(User user, boolean isBlocked) {
        boolean withdrawn = user.getDeletedAt() != null;

        if (withdrawn) {
            return OpponentUserResponse.builder()
                    .opponentUserId(user.getId())
                    .nickname(null)
                    .profileImageUrl(null)
                    .emailVerified(false)
                    .blocked(isBlocked)
                    .withdrawn(true)
                    .build();
        }

        return OpponentUserResponse.builder()
                .opponentUserId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfile_img())
                .emailVerified(user.isEmail_verified())
                .blocked(isBlocked)
                .withdrawn(false)
                .build();
    }

    private ChatRoomConverter() {}
}
