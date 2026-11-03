package com.fmi.domain.post.post.web.dto.response;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import java.time.LocalDateTime;

public record PostBriefResponse(
        Long id,
        String title,
        String summary,
        String thumbnailImageUrl,
        String address,
        PostStatus postStatus,
        PostType postType,
        Category category,
        Long favoriteCount,
        boolean favoriteStatus,
        Long viewCount,
        boolean isNew,
        boolean isHot,
        LocalDateTime createdAt,
        Integer imageCount) {}
