package com.fmi.domain.auth.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordChangeRequest")
class PasswordChangeRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("새 비밀번호가 비어 있지 않으면")
    class WithNonBlankNewPassword {

        @Test
        @DisplayName("정책 위반 여부는 Bean Validation이 아닌 비밀번호 유스케이스가 판단한다")
        void leavesWeakPasswordValidationToPasswordUseCase() {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setNewPassword("short");
            request.setNewPasswordConfirm("short");

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }
    }
}
