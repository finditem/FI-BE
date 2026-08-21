package com.fmi.domain.chatroom.converter;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.*;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.post.data.Post;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatRoomConverter {

    public static ChatRoomResultDTO toChatRoomResultDTO(
            ChatRoom chatRoom, User user, Post post, Long unreadCount, String thumbnailUrl, boolean isBlocked) {

        var opponentUser = buildOpponentUserDTO(user, isBlocked);

        var postInfo = PostInfoDTO.builder()
                .postId(post.getId())
                .postType(post.getPostType())
                .category(post.getCategory())
                .title(post.getTitle())
                .address(post.getAddress())
                .thumbnailUrl(thumbnailUrl)
                .postStatus(post.getPostStatus())
                .deleted(post.isDeleted())
                .build();

        return ChatRoomResultDTO.builder()
                .roomId(chatRoom.getId())
                .unreadCount(unreadCount)
                .opponentUser(opponentUser)
                .postInfo(postInfo)
                .build();
    }

    public static ChatRoomResultDTO toChatRoomResultDTOFromSnapshot(
            ChatRoom chatRoom, User opponent, Long unreadCount, boolean isBlocked) {

        var opponentUser = buildOpponentUserDTO(opponent, isBlocked);

        var postInfo = PostInfoDTO.builder()
                .postId(chatRoom.getSourcePostId())
                .postType(chatRoom.getSourcePostType())
                .category(chatRoom.getSourceCategory())
                .title(chatRoom.getSourceTitle())
                .address(chatRoom.getSourceAddress())
                .thumbnailUrl(chatRoom.getSourceThumbnailUrl())
                .postStatus(chatRoom.getSourcePostStatus())
                .deleted(true)
                .build();

        return ChatRoomResultDTO.builder()
                .roomId(chatRoom.getId())
                .unreadCount(unreadCount)
                .opponentUser(opponentUser)
                .postInfo(postInfo)
                .build();
    }

    public static List<ChatRoomSummaryDTO> toChatRoomSummaryListDTO(
            List<ChatRoomParticipant> participants, User currentUser, Map<Long, String> thumbnailMap) {
        return participants.stream()
                .map(pt -> {
                    User contactUser = pt.getChatRoom().getOtherParticipant(currentUser.getId());

                    return toChatRoomSummaryDTO(pt, contactUser, thumbnailMap);
                })
                .collect(Collectors.toList());
    }

    public static ChatRoomSummaryDTO toChatRoomSummaryDTO(
            ChatRoomParticipant participant, User contactUser, Map<Long, String> thumbnailMap) {

        ContactUserDTO contactUserDTO = buildContactUserDTO(contactUser);

        ChatRoom room = participant.getChatRoom();

        PostInfoDTO postInfoDTO;
        if (!room.isSourcePostDeleted() && room.getPost() != null) {
            Post post = room.getPost();
            postInfoDTO = PostInfoDTO.builder()
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
            postInfoDTO = PostInfoDTO.builder()
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

        return ChatRoomSummaryDTO.builder()
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

    private static ContactUserDTO buildContactUserDTO(User user) {
        boolean withdrawn = user.getDeletedAt() != null;

        if (withdrawn) {
            return ContactUserDTO.builder()
                    .userId(user.getId())
                    .nickname(null)
                    .profileImageUrl(null)
                    .withdrawn(true)
                    .build();
        }

        return ContactUserDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfile_img())
                .withdrawn(false)
                .build();
    }

    private static opponentUserDTO buildOpponentUserDTO(User user, boolean isBlocked) {
        boolean withdrawn = user.getDeletedAt() != null;

        if (withdrawn) {
            return opponentUserDTO
                    .builder()
                    .opponentUserId(user.getId())
                    .nickname(null)
                    .profileImageUrl(null)
                    .emailVerified(false)
                    .blocked(isBlocked)
                    .withdrawn(true)
                    .build();
        }

        return opponentUserDTO
                .builder()
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
