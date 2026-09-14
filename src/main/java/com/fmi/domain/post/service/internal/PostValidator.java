package com.fmi.domain.post.service.internal;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostValidator {

    private final Clock clock;

    public void validateDate(LocalDateTime date) {
        if (date != null && date.isAfter(LocalDateTime.now(clock))) {
            throw new GeneralException(ErrorStatus._POST_DATE_IN_FUTURE);
        }
    }
}
