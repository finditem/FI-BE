package com.fmi.domain.review.data;

import com.fmi.domain.review.data.enums.ReviewEmotion;
import com.fmi.domain.review.data.enums.ReviewHelpType;
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
