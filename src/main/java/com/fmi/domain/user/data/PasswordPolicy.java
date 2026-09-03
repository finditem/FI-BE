package com.fmi.domain.user.data;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;

public final class PasswordPolicy {

    private PasswordPolicy() {}

    public static void validate(String rawPassword) {
        String password = rawPassword == null ? "" : rawPassword;
        boolean valid = password.length() >= 8
                && password.length() <= 16
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*[0-9].*")
                && password.matches(".*[!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?].*");
        if (!valid) {
            throw new GeneralException(ErrorStatus._WEAK_PASSWORD);
        }
    }
}
