package com.fmi.domain.comment.web.dto.response;

import com.fmi.domain.user.web.dto.response.UserCommentResponse;
import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String content,
        boolean deleted,
        int depth,
        LocalDateTime createdAt,
        UserCommentResponse authorResponse,
        long childCommentCount,
        List<CommentImageResponse> imageList,
        long likeCount,
        boolean isLike,
        boolean canEdit,
        boolean canDelete) {}
