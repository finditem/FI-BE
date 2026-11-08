package com.fmi.domain.post.findcompletion.service.internal;

import com.fmi.domain.chat.room.data.ChatRoom;
import com.fmi.domain.post.findcompletion.exception.FindCompletionErrorStatus;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.user.block.service.BlockService;
import com.fmi.domain.user.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FindCompletionValidator {

    private final BlockService blockService;

    public void validate(ChatRoom chatRoom, User requester) {
        if (!chatRoom.isParticipant(requester)) {
            throw new GeneralException(ErrorStatus._CHATROOM_ACCESS_DENIED);
        }

        Post post = chatRoom.getPost();
        if (post == null || chatRoom.isSourcePostDeleted() || post.isDeleted()) {
            throw new GeneralException(ErrorStatus._POST_NOT_FOUND);
        }
        if (post.getPostType() != PostType.LOST) {
            throw new GeneralException(FindCompletionErrorStatus.INVALID_POST_TYPE);
        }
        if (!post.getUser().getId().equals(requester.getId())) {
            throw new GeneralException(FindCompletionErrorStatus.ACCESS_DENIED);
        }
        if (post.getPostStatus() != PostStatus.SEARCHING) {
            throw new GeneralException(FindCompletionErrorStatus.ALREADY_COMPLETED);
        }

        User helper = chatRoom.getOtherParticipant(requester.getId());
        if (helper.getDeletedAt() != null || blockService.isBlocked(requester.getId(), helper.getId())) {
            throw new GeneralException(FindCompletionErrorStatus.NOT_ALLOWED);
        }
    }
}
