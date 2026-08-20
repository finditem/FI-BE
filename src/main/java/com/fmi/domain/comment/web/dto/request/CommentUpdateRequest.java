package com.fmi.domain.comment.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CommentUpdateRequest(@NotBlank String content, List<Long> deleteImageIds) {}
