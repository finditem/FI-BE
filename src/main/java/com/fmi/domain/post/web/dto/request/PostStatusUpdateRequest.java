package com.fmi.domain.post.web.dto.request;

import com.fmi.domain.post.data.PostStatus;
import jakarta.validation.constraints.NotNull;

public record PostStatusUpdateRequest(@NotNull PostStatus postStatus) {}
