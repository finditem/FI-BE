package com.fmi.domain.post.web.dto.response;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;

import java.time.LocalDateTime;

public record PostBriefResponse(
        Long id,
        String title,
        String summary,
        PostImageResponse thumbnailImage,
        String address,
        PostStatus postStatus,
        PostType postType,
        Category category,
        Long favoriteCount,
        boolean favoriteStatus,
        Long viewCount,
        boolean isNew,
        boolean isHot,
        LocalDateTime createdAt) {
}
