package com.fmi.domain.post.web.dto.request;

import com.fmi.domain.post.data.Radius;
import jakarta.validation.constraints.NotNull;

public record PostRadiusUpdateRequest(@NotNull Radius radius) {}
