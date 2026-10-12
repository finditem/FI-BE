package com.fmi.domain.chatroom.web.dto;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.findcompletion.data.FindCompletionStatus;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatRoomResponseDTO {

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ChatRoomResultDTO {
        private Long roomId;
        private Long unreadCount;
        private opponentUserDTO opponentUser;
        private PostInfoDTO postInfo;
        private FindCompletionInfoDTO findCompletion;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class FindCompletionInfoDTO {
        @Schema(description = "NONE(채팅방에서 완료 전), COMPLETED_HERE(이 채팅방에서 완료), COMPLETED_ELSEWHERE(같은 게시글의 다른 채팅방에서 완료)")
        private FindCompletionStatus status;

        @Schema(description = "찾기 완료 id. COMPLETED_HERE일 때만 값이 있고, 후기 작성 API에 사용합니다.")
        private Long findCompletionId;

        @Schema(description = "[분실물 찾기 완료] 버튼 노출 여부")
        private boolean canComplete;

        @Schema(description = "[후기 작성하기] 버튼 노출 여부")
        private boolean canWriteReview;
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
        private boolean blocked;
        private boolean withdrawn;
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
        private boolean withdrawn;
    }
}
