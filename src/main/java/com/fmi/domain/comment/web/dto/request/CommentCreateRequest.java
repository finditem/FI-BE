package com.fmi.domain.comment.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequest(
        @NotNull
        Long postId,

        @NotBlank
        String content,

        Long parentId
) {
}
