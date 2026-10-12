package com.fmi.domain.review.data;

import com.fmi.domain.user.data.User;
import java.time.LocalDateTime;

public record ReviewUser(Long userId, String nickname, String profileImageUrl, LocalDateTime deletedAt) {

    // 30일 뒤 하드 삭제되면 FK가 null이 되어 사용자가 없다.
    public static ReviewUser from(User user) {
        if (user == null) {
            return new ReviewUser(null, null, null, null);
        }
        return new ReviewUser(user.getId(), user.getNickname(), user.getProfile_img(), user.getDeletedAt());
    }

    public boolean isWithdrawn() {
        return userId == null || deletedAt != null;
    }
}
