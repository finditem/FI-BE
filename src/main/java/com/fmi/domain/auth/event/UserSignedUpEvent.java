package com.fmi.domain.auth.event;

import com.fmi.domain.user.data.User;
import java.time.LocalDateTime;

public record UserSignedUpEvent(Long userId, String email, String nickname, LocalDateTime signedUpAt) {

    public static UserSignedUpEvent from(User user) {
        return new UserSignedUpEvent(user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt());
    }
}
