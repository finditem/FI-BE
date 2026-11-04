package com.fmi.domain.post.review.data;

import java.time.LocalDateTime;

public record ReviewPostInfo(
        Long postId,
        String title,
        String address,
        String thumbnailUrl,
        LocalDateTime date,
        LocalDateTime createdAt,
        boolean deleted) {}
