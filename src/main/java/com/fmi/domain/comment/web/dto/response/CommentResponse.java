package com.fmi.domain.comment.web.dto.response;

import com.fmi.domain.user.web.dto.response.UserCommentResponse;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        boolean deleted,
        int depth,
        LocalDateTime createdAt,
        UserCommentResponse authorResponse,
        long replyCount,
        Long nextReplyCursor,
        List<CommentImageResponse> imageList,
        List<CommentResponse> childrenCommentList,
        long likeCount,
        boolean isLike
) {
}
