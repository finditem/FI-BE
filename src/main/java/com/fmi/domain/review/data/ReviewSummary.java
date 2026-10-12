package com.fmi.domain.review.data;

import java.time.LocalDateTime;

public record ReviewSummary(
        Long reviewId,
        ReviewEmotion emotion,
        String content,
        LocalDateTime createdAt,
        Long userId,
        String nickname,
        String profileImageUrl,
        LocalDateTime userDeletedAt) {

    public ReviewUser user() {
        return new ReviewUser(userId, nickname, profileImageUrl, userDeletedAt);
    }

    public boolean isUserWithdrawn() {
        return user().isWithdrawn();
    }
}
