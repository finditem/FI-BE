package com.fmi.domain.review.data;

import java.time.LocalDateTime;
import java.util.Set;

public record ReviewDetail(
        Long reviewId,
        ReviewEmotion emotion,
        Set<ReviewHelpType> helpTypes,
        String content,
        LocalDateTime createdAt,
        ReviewUser author,
        ReviewUser recipient,
        ReviewPostInfo post,
        Long roomId) {}
