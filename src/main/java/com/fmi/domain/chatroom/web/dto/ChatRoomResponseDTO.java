package com.fmi.domain.chatroom.web.dto;

import com.fmi.domain.Enum.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    }
}
