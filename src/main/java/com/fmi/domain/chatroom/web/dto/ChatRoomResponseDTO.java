package com.fmi.domain.chatroom.web.dto;

import com.fmi.domain.Enum.Type;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomResponseDTO {

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ChatRoomResultDTO{
        private Long roomId;
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
        private Type postType;
        private String title;
        private String address;
        private String thumbnailUrl;
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
