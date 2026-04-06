package com.fmi.domain.chatroom.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.post.data.Post;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.*;

public class ChatRoomConverter {

    public static ChatRoomResultDTO toChatRoomResultDTO(ChatRoom chatRoom, User user, Post post, Long unreadCount, String thumbnailUrl, boolean isBlocked) {

        var opponentUser = opponentUserDTO.builder()
                .opponentUserId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfile_img() == null ? null : user.getProfile_img())
                .emailVerified(user.isEmail_verified())
                .blocked(isBlocked)
                .build();

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

    public static ChatRoomResultDTO toChatRoomResultDTOFromSnapshot(ChatRoom chatRoom, User opponent, Long unreadCount, boolean isBlocked) {

        var opponentUser = opponentUserDTO.builder()
                .opponentUserId(opponent.getId())
                .nickname(opponent.getNickname())
                .profileImageUrl(opponent.getProfile_img())
                .emailVerified(opponent.isEmail_verified())
                .blocked(isBlocked)
                .build();

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

    public static List<ChatRoomSummaryDTO> toChatRoomSummaryListDTO(List<ChatRoomParticipant> participants, User currentUser, Map<Long, String> thumbnailMap) {
        return participants.stream()
                .map(pt -> {
                    User contactUser = pt.getChatRoom().getOtherParticipant(currentUser.getId());

                    return toChatRoomSummaryDTO(pt, contactUser, thumbnailMap);
                })
                .collect(Collectors.toList());
    }


    public static ChatRoomSummaryDTO toChatRoomSummaryDTO(ChatRoomParticipant participant, User contactUser, Map<Long, String> thumbnailMap) {

        ContactUserDTO contactUserDTO = ContactUserDTO.builder()
                .userId(contactUser.getId())
                .nickname(contactUser.getNickname())
                .profileImageUrl(contactUser.getProfile_img())
                .build();

        ChatRoom room = participant.getChatRoom();

        PostInfoDTO postInfoDTO;
        if (!room.isSourcePostDeleted()) {
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


    private ChatRoomConverter() {
    }
}
