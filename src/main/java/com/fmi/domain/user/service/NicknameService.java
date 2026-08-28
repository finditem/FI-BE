package com.fmi.domain.user.service;

import com.fmi.domain.user.service.internal.NicknameValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NicknameService {

    private final NicknameValidator nicknameValidator;

    public CheckResult check(String nickname) {
        NicknameValidator.ValidationResult result = nicknameValidator.validateAvailable(nickname);
        if (result.valid()) {
            return CheckResult.success();
        }
        return result.failure() == NicknameValidator.Failure.DUPLICATE
                ? CheckResult.duplicate("중복된 닉네임입니다")
                : CheckResult.invalid("부적절한 닉네임입니다");
    }

    public record CheckResult(boolean available, String errorType, String message) {

        public static CheckResult success() {
            return new CheckResult(true, null, null);
        }

        public static CheckResult invalid(String message) {
            return new CheckResult(false, "INVALID", message);
        }

        public static CheckResult duplicate(String message) {
            return new CheckResult(false, "DUPLICATE", message);
        }
    }
}
