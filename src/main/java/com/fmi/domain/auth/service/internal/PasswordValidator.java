package com.fmi.domain.auth.service.internal;

import com.fmi.domain.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordValidator {
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public void validateConfirmation(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new GeneralException(ErrorStatus._PASSWORD_CONFIRMATION_MISMATCH);
        }
    }

    public CurrentPasswordValidationResult validateCurrentPassword(User user, String rawPassword) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (user.hasActiveTemporaryPassword(now)) {
            if (passwordEncoder.matches(rawPassword, user.getTemporaryPassword())) {
                return CurrentPasswordValidationResult.TEMPORARY;
            }
            if (user.getOriginalPassword() != null
                    && passwordEncoder.matches(rawPassword, user.getOriginalPassword())) {
                return CurrentPasswordValidationResult.STANDARD;
            }
            return CurrentPasswordValidationResult.FAILED;
        }

        if (user.hasExpiredTemporaryPassword(now)) {
            boolean matchesOriginal = user.getOriginalPassword() != null
                    && passwordEncoder.matches(rawPassword, user.getOriginalPassword());
            return matchesOriginal ? CurrentPasswordValidationResult.STANDARD : CurrentPasswordValidationResult.FAILED;
        }

        return passwordEncoder.matches(rawPassword, user.getPassword())
                ? CurrentPasswordValidationResult.STANDARD
                : CurrentPasswordValidationResult.FAILED;
    }

    public enum CurrentPasswordValidationResult {
        STANDARD,
        TEMPORARY,
        FAILED
    }
}
