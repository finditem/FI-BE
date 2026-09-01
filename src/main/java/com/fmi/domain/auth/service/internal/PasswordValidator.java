package com.fmi.domain.auth.service.internal;

import com.fmi.domain.user.data.PasswordPolicy;
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

    public void validateNewPassword(String rawPassword) {
        PasswordPolicy.validate(rawPassword);
    }

    public void validateConfirmation(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new GeneralException(ErrorStatus._PASSWORD_CONFIRMATION_MISMATCH);
        }
    }

    public boolean matchesActiveTemporaryPassword(User user, String rawPassword) {
        LocalDateTime now = LocalDateTime.now(clock);
        return user.hasActiveTemporaryPassword(now)
                && passwordEncoder.matches(rawPassword, user.getTemporaryPassword());
    }

    public void validateLoginPassword(User user, String rawPassword) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (user.hasActiveTemporaryPassword(now) || user.hasExpiredTemporaryPassword(now)) {
            if (user.getOriginalPassword() == null
                    || !passwordEncoder.matches(rawPassword, user.getOriginalPassword())) {
                throw new GeneralException(ErrorStatus._INVALID_CREDENTIALS);
            }
            return;
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new GeneralException(ErrorStatus._INVALID_CREDENTIALS);
        }
    }

    public void validateAccountPassword(User user, String rawPassword) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (user.hasActiveTemporaryPassword(now) || user.hasExpiredTemporaryPassword(now)) {
            if (user.getOriginalPassword() == null
                    || !passwordEncoder.matches(rawPassword, user.getOriginalPassword())) {
                throw new GeneralException(ErrorStatus._CURRENT_PASSWORD_INCORRECT);
            }
            return;
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new GeneralException(ErrorStatus._CURRENT_PASSWORD_INCORRECT);
        }
    }
}
