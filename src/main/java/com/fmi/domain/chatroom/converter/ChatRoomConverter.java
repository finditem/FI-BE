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
                .opponentUserId(user.getUserId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfile_img())
                .emailVerified(user.isEmail_verified())
                .build();

        var postInfo = PostInfoDTO.builder()
                .postId(post.getPostId())
                .postType(post.getPost_type())
                .title(post.getTitle())
                .build();

        return ChatRoomResultDTO.builder()
                .roomId(chatRoom.getChatroom_id())
                .opponentUser(opponentUser)
                .postInfo(postInfo)
                .build();
    }

    public static List<ChatRoomSummaryDTO> toChatRoomSummaryListDTO(List<ChatRoom> chatRooms) {
        return chatRooms.stream()
                .map(ChatRoomConverter::toChatRoomSummaryDTO)
                .collect(Collectors.toList());
    }

    public static ChatRoomSummaryDTO toChatRoomSummaryDTO(ChatRoom chatRoom) {
        User contactUser = chatRoom.getUser();

        ContactUserDTO contactUserDTO = ContactUserDTO.builder()
                .userId(contactUser.getUserId())
                .nickname(contactUser.getNickname())
                .profileImageUrl(contactUser.getProfile_img())
                .build();

        return ChatRoomSummaryDTO.builder()
                .roomId(chatRoom.getChatroom_id())
                .contactUser(contactUserDTO)
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageSentAt(chatRoom.getUpdatedAt())
                .build();
    }

}
