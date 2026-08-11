package com.fmi.domain.post.web.dto.request;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PostCreateRequest(
        @NotNull
        PostType postType,

        @NotBlank
        @Size(min = 1, max = 50, message = "제목은 최대 50자까지 입력 가능합니다.")
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
        @Size(min = 1, max = 500, message = "본문은 최대 500자까지 입력 가능합니다.")
        String content,

        @NotNull
        Radius radius,

        @NotNull
        Category category
) {
}