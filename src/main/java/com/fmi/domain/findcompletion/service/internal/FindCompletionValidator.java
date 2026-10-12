package com.fmi.domain.findcompletion.service.internal;

import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.findcompletion.exception.FindCompletionErrorStatus;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import org.springframework.stereotype.Component;

@Component
public class FindCompletionValidator {

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
    }
}
