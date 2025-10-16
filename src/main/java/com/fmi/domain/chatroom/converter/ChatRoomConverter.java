package com.fmi.domain.chatroom.converter;

import com.fmi.domain.User;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.post.data.Post;

import java.util.List;
import java.util.stream.Collectors;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.*;

public class ChatRoomConverter {
    public static ChatRoomResultDTO toChatRoomResultDTO(ChatRoom chatRoom, User user, Post post) {

        var opponentUser = opponentUserDTO.builder()
                .opponentUserId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfile_img())
                .emailVerified(user.isEmail_verified())
                .build();

        var postInfo = PostInfoDTO.builder()
                .postId(post.getId())
                .postType(post.getPostType())
                .title(post.getTitle())
                .build();

        return ChatRoomResultDTO.builder()
                .roomId(chatRoom.getId())
                .opponentUser(opponentUser)
                .postInfo(postInfo)
                .build();
    }

    public static List<ChatRoomSummaryDTO> toChatRoomSummaryListDTO(List<ChatRoom> chatRooms, Long currentUserId) {
        return chatRooms.stream()
                .map(chatRoom -> toChatRoomSummaryDTO(chatRoom, currentUserId))
                .collect(Collectors.toList());
    }

    public static ChatRoomSummaryDTO toChatRoomSummaryDTO(ChatRoom chatRoom, Long currentUserId) {

        User contactUser = chatRoom.getOtherParticipant(currentUserId);

        ContactUserDTO contactUserDTO = ContactUserDTO.builder()
                .userId(contactUser.getId())
                .nickname(contactUser.getNickname())
                .profileImageUrl(contactUser.getProfile_img())
                .build();

        return ChatRoomSummaryDTO.builder()
                .roomId(chatRoom.getId())
                .contactUser(contactUserDTO)
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageSentAt(chatRoom.getUpdatedAt())
                .build();
    }

}
