package com.fmi.domain.post.web.dto.request;

import com.fmi.domain.post.data.enums.PostStatus;
import jakarta.validation.constraints.NotNull;

public record PostStatusUpdateRequest(@NotNull PostStatus postStatus) {}
