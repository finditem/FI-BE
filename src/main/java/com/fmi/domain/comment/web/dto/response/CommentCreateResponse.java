package com.fmi.domain.comment.web.dto.response;

import com.fmi.domain.user.web.dto.response.UserCommentResponse;
import java.time.LocalDateTime;
import java.util.List;

public record CommentCreateResponse(
        Long id,
        String content,
        LocalDateTime createdAt,
        int likeCount,
        boolean canEdit,
        boolean canDelete,
        UserCommentResponse authorResponse,
        List<CommentImageResponse> commentImageResponseList) {}
