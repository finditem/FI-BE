package com.fmi.domain.auth.service.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.domain.auth.data.RejoinType;
import com.fmi.domain.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RejoinPolicyTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-22T03:00:00Z"), SEOUL_ZONE);
    private static final String TEST_ACCOUNT_EMAIL = "test@test.com";

    @Nested
    @DisplayName("7일 이내 탈퇴한 계정")
    class RecentlyWithdrawnAccount {

        @Test
        @DisplayName("local과 dev에 설정된 카카오 테스트 계정이면 재가입을 허용한다")
        void allowsConfiguredKakaoTestAccount() {
            User user = withdrawnUser(TEST_ACCOUNT_EMAIL, LocalDateTime.of(2026, 9, 21, 12, 0));

            assertThatCode(() -> policy(Set.of(TEST_ACCOUNT_EMAIL)).validate(user, RejoinType.KAKAO))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("일반 회원가입이면 테스트 계정 이메일도 재가입을 차단한다")
        void blocksEmailSignupForTestAccount() {
            User user = withdrawnUser(TEST_ACCOUNT_EMAIL, LocalDateTime.of(2026, 9, 21, 12, 0));

            assertRecentlyDeleted(() -> policy(Set.of(TEST_ACCOUNT_EMAIL)).validate(user, RejoinType.EMAIL));
        }
    }

    @Test
    @DisplayName("탈퇴 후 7일이 지났으면 재가입을 허용한다")
    void allowsAccountWithdrawnMoreThanSevenDaysAgo() {
        User user = withdrawnUser("user@example.com", LocalDateTime.of(2026, 9, 14, 12, 0));

        assertThatCode(() -> policy(Set.of()).validate(user, RejoinType.EMAIL)).doesNotThrowAnyException();
    }

    private RejoinPolicy policy(Set<String> testAccountEmails) {
        return new RejoinPolicy(CLOCK, testAccountEmails);
    }

    private User withdrawnUser(String email, LocalDateTime deletedAt) {
        return User.builder().email(email).deletedAt(deletedAt).build();
    }

    private void assertRecentlyDeleted(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(GeneralException.class, exception -> org.assertj.core.api.Assertions.assertThat(
                                exception.getCode())
                        .isEqualTo(ErrorStatus._EMAIL_RECENTLY_DELETED));
    }
}
