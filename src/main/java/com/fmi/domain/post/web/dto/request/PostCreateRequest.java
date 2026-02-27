package com.fmi.domain.post.web.dto.request;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PostCreateRequest(
        @NotNull
        PostType postType,

        @NotBlank
        String title,

        @NotNull
        LocalDateTime date,

        @NotBlank
        String address,

        @NotNull
        Double latitude,

        @NotNull
        Double longitude,

        @NotBlank
        String content,

        @NotNull
        Radius radius,

        @NotNull
        Category category
) {
}