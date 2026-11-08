package com.fmi.domain.post.post.web.dto.request;

import com.fmi.domain.post.post.data.enums.Radius;
import jakarta.validation.constraints.NotNull;

public record PostRadiusUpdateRequest(@NotNull Radius radius) {}
