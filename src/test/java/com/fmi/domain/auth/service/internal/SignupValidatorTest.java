package com.fmi.domain.auth.service.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignupValidator")
class SignupValidatorTest {

    private static final String EMAIL = "member@finditem.kr";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-23T03:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("회원가입 이메일을 검증할 때")
    class DescribeValidate {

        @Nested
        @DisplayName("활성 계정 이메일이면")
        class ContextWithDuplicatedEmail {

            @Test
            @DisplayName("중복 이메일 예외를 던진다")
            void itThrowsDuplicatedEmail() {
                // given
                when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

                // when & then
                assertThatThrownBy(() -> validator().validate(EMAIL))
                        .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus._EMAIL_DUPLICATED));
            }
        }

        @Nested
        @DisplayName("7일 이내 탈퇴한 이메일이면")
        class ContextWithRecentlyDeletedEmail {

            @Test
            @DisplayName("재가입 제한 예외를 던진다")
            void itThrowsRecentlyDeletedEmail() {
                // given
                LocalDateTime blockedSince = LocalDateTime.of(2026, 8, 16, 3, 0);
                when(userRepository.existsRecentlyDeletedByEmail(EMAIL, blockedSince))
                        .thenReturn(true);

                // when & then
                assertThatThrownBy(() -> validator().validate(EMAIL))
                        .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus._EMAIL_RECENTLY_DELETED));
                verify(userRepository).existsRecentlyDeletedByEmail(EMAIL, blockedSince);
            }
        }
    }

    private SignupValidator validator() {
        return new SignupValidator(userRepository, CLOCK);
    }
}
