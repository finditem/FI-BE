package com.fmi.domain.post.web.dto.request;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import com.fmi.domain.post.web.dto.validation.ValidThumbnailKeep;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@ValidThumbnailKeep
public record PostUpdateRequest(
        PostType postType,

        @Size(min = 1)
        String title,

        PostStatus postStatus,

        LocalDateTime date,

        @Size(min = 1)
        String address,

        Double latitude,

        Double longitude,

        @Size(min = 1)
        String content,

        Boolean temporarySave,

        Radius radius,

        Category category,

        List<@Positive(message = "이미지 ID는 양수여야 합니다.") Long> keepImageIdList,

        Long thumbnailImageId) {
}
