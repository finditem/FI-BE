package com.fmi.domain.chatroom.web.dto;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomResponseDTO {

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ChatRoomResultDTO{
        private Long roomId;
        private Long unreadCount;
        private opponentUserDTO opponentUser;
        private PostInfoDTO postInfo;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class opponentUserDTO {
        private Long opponentUserId;
        private String nickname;
        private String profileImageUrl;
        private boolean emailVerified;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class PostInfoDTO {
        private Long postId;
        private PostType postType;
        private Category category;
        private String title;
        private String address;
        private String thumbnailUrl;
        private PostStatus postStatus;
        private boolean deleted;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MyChatListDTO {
        private List<ChatRoomSummaryDTO> chatRooms;
        private Long nextCursor;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ChatRoomSummaryDTO {
        private Long roomId;
        private ContactUserDTO contactUser;
        private PostInfoDTO postInfo;
        private MessageType messageType;
        private String lastMessage;
        private LocalDateTime lastMessageSentAt;
        private Long unreadCount;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ContactUserDTO {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
    }
}
