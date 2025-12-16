package com.fmi.domain.chatroom.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatmessage.data.ChatMessage;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.post.data.Post;

import java.util.List;
import java.util.stream.Collectors;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.*;

public class ChatRoomConverter {
    public static ChatRoomResultDTO toChatRoomResultDTO(ChatRoom chatRoom, User user, Post post) {

        String thumbnailUrl = post.getImages().isEmpty() ? null : post.getImages().get(0).getImgUrl();

        var opponentUser = opponentUserDTO.builder()
                .opponentUserId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfile_img() == null ? null : user.getProfile_img())
                .emailVerified(user.isEmail_verified())
                .build();

        var postInfo = PostInfoDTO.builder()
                .postId(post.getId())
                .postType(post.getPostType())
                .title(post.getTitle())
                .address(post.getAddress())
                .thumbnailUrl(thumbnailUrl)
                .build();

        return ChatRoomResultDTO.builder()
                .roomId(chatRoom.getId())
                .opponentUser(opponentUser)
                .postInfo(postInfo)
                .build();
    }

    public static List<ChatRoomSummaryDTO> toChatRoomSummaryListDTO(List<ChatRoomParticipant> participants, User currentUser) {
        return participants.stream()
                .map(pt -> {
                    User contactUser = pt.getChatRoom().getOtherParticipant(currentUser.getId());

                    return toChatRoomSummaryDTO(pt, contactUser);
                })
                .collect(Collectors.toList());
    }


    public static ChatRoomSummaryDTO toChatRoomSummaryDTO(ChatRoomParticipant participant, User contactUser) {

        ContactUserDTO contactUserDTO = ContactUserDTO.builder()
                .userId(contactUser.getId())
                .nickname(contactUser.getNickname())
                .profileImageUrl(contactUser.getProfile_img())
                .build();

        Post post = participant.getChatRoom().getPost();
        String thumbnailUrl = post.getImages().isEmpty() ? null : post.getImages().get(0).getImgUrl();

        PostInfoDTO postInfoDTO = PostInfoDTO.builder()
                .postId(post.getId())
                .postType(post.getPostType())
                .title(post.getTitle())
                .address(post.getAddress())
                .thumbnailUrl(thumbnailUrl)
                .build();

        ChatMessage lastMessage = participant.getLastMessage();
        String content = null;
        MessageType messageType = null;

        if (lastMessage != null) {
            content = lastMessage.getContent();
            messageType=lastMessage.getMessageType();
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
}