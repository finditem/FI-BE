package com.fmi.domain.post.web.dto.request;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;


public record PostUpdateRequest(
        PostType postType,

        @Size(min = 1)
        String title,

        PostStatus postStatus,

        LocalDate date,

        @Size(min = 1)
        String address,

        Double latitude,

        Double longitude,

        @Size(min = 1)
        String content,

        Boolean temporarySave,

        Radius radius,

        Category category,

        List<@Positive(message = "이미지 ID는 양수여야 합니다.") Long> deleteImageIdList) {
}
