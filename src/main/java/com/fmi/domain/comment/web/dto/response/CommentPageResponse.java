package com.fmi.domain.comment.web.dto.response;

import java.util.List;

public record CommentPageResponse(
        List<CommentResponse> comments,
        boolean hasNext,
        Integer nextPage,
        long remainingCount) {
}
