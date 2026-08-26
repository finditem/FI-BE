package com.fmi.domain.auth.service.internal;

import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignupValidator {

    private final UserRepository userRepository;
    private final Clock clock;

    public void validate(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorStatus._EMAIL_DUPLICATED);
        }

        LocalDateTime reSignupBlockedSince = LocalDateTime.now(clock).minusDays(7);
        if (userRepository.existsRecentlyDeletedByEmail(email, reSignupBlockedSince)) {
            throw new GeneralException(ErrorStatus._EMAIL_RECENTLY_DELETED);
        }
    }
}
