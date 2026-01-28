package com.fmi.domain.post.web.dto.request;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PostCreateRequest(

        @NotNull
        PostType postType,

        @NotBlank
        String title,

        @NotNull
        LocalDate date,

        @NotBlank
        String address,

        Double latitude,

        Double longitude,

        @NotBlank
        String content,

        @NotNull
        Radius radius,

        @NotNull
        Category category,

        boolean temporarySave
) {
}