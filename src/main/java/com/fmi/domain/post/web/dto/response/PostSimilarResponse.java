package com.fmi.domain.post.web.dto.response;

import com.fmi.domain.post.data.enums.Category;
import java.time.LocalDateTime;

public record PostSimilarResponse(
        Long postId,
        String title,
        String thumbnailImageUrl,
        String address,
        Long favoriteCount,
        boolean favoriteStatus,
        Long viewCount,
        LocalDateTime createdAt,
        Category category) {}
