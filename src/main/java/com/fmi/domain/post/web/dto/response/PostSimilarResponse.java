package com.fmi.domain.post.web.dto.response;

import java.time.LocalDateTime;

public record PostSimilarResponse(
        Long postId,
        String title,
        String thumbnailImageUrl,
        String address,
        Long favoriteCount,
        boolean favoriteStatus,
        Long viewCount,
        LocalDateTime createdAt
) {
}
