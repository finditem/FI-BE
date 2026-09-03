package com.fmi.domain.user.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordPolicy")
class PasswordPolicyTest {

    @Nested
    @DisplayName("비밀번호를 검증할 때")
    class DescribeValidate {

        @Test
        @DisplayName("정책에 맞는 비밀번호면 통과한다")
        void itAcceptsValidPassword() {
            assertThatCode(() -> PasswordPolicy.validate("Password1!")).doesNotThrowAnyException();
        }

        @Nested
        @DisplayName("정책에 맞지 않는 비밀번호면")
        class ContextWithWeakPassword {

            @Test
            @DisplayName("약한 비밀번호 예외를 던진다")
            void itThrowsWeakPassword() {
                assertThatThrownBy(() -> PasswordPolicy.validate("short"))
                        .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus._WEAK_PASSWORD));
            }
        }
    }
}
