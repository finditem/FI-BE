package com.fmi.domain.user.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.Enum.WithdrawalReason;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("User")
class UserTest {

    @Nested
    @DisplayName("임시 비밀번호 발급")
    class IssueTemporaryPassword {

        @Test
        @DisplayName("재발급해도 최초 비밀번호는 보존한다")
        void preservesOriginalPassword() {
            // given
            LocalDateTime now = LocalDateTime.of(2026, 8, 23, 12, 0);
            User user = User.builder().password("encoded-original-password").build();

            user.issueTemporaryPassword("encoded-first-temporary-password", now.plusHours(1), now);

            LocalDateTime reissuedAt = now.plusMinutes(10);
            LocalDateTime reissuedExpiresAt = reissuedAt.plusHours(1);

            // when
            user.issueTemporaryPassword("encoded-second-temporary-password", reissuedExpiresAt, reissuedAt);

            // then
            assertThat(user.getOriginalPassword()).isEqualTo("encoded-original-password");
            assertThat(user.getTemporaryPassword()).isEqualTo("encoded-second-temporary-password");
            assertThat(user.getTemporaryPasswordExpiresAt()).isEqualTo(reissuedExpiresAt);
            assertThat(user.getPassword()).isEqualTo("encoded-second-temporary-password");
            assertThat(user.getUpdatedAt()).isEqualTo(reissuedAt);
        }
    }

    @Nested
    @DisplayName("임시 비밀번호 복원")
    class RestoreExpiredTemporaryPassword {

        @Nested
        @DisplayName("만료 시각이 되었을 때")
        class WhenExpired {

            @Test
            @DisplayName("원래 비밀번호를 복원하고 임시 상태를 제거한다")
            void restoresOriginalPasswordAndClearsTemporaryState() {
                // given
                LocalDateTime issuedAt = LocalDateTime.of(2026, 8, 23, 12, 0);
                LocalDateTime expiresAt = issuedAt.plusHours(1);
                User user = User.builder().password("encoded-original-password").build();
                user.issueTemporaryPassword("encoded-temporary-password", expiresAt, issuedAt);

                // when
                boolean restored = user.restoreExpiredTemporaryPassword(expiresAt);

                // then
                assertThat(restored).isTrue();
                assertThat(user.getPassword()).isEqualTo("encoded-original-password");
                assertThat(user.getOriginalPassword()).isNull();
                assertThat(user.getTemporaryPassword()).isNull();
                assertThat(user.getTemporaryPasswordExpiresAt()).isNull();
                assertThat(user.getUpdatedAt()).isEqualTo(expiresAt);
            }
        }
    }

    @Nested
    @DisplayName("비밀번호를 변경할 때")
    class ChangePassword {

        @Test
        @DisplayName("임시 비밀번호 상태를 모두 제거한다")
        void clearsTemporaryPasswordState() {
            // given
            LocalDateTime issuedAt = LocalDateTime.of(2026, 8, 23, 12, 0);
            User user = User.builder().password("encoded-original-password").build();
            user.issueTemporaryPassword("encoded-temporary-password", issuedAt.plusHours(1), issuedAt);

            LocalDateTime changedAt = issuedAt.plusMinutes(10);

            // when
            user.changePassword("encoded-new-password", changedAt);

            // then
            assertThat(user.getPassword()).isEqualTo("encoded-new-password");
            assertThat(user.getOriginalPassword()).isNull();
            assertThat(user.getTemporaryPassword()).isNull();
            assertThat(user.getTemporaryPasswordExpiresAt()).isNull();
            assertThat(user.getUpdatedAt()).isEqualTo(changedAt);
        }
    }

    @Nested
    @DisplayName("약관 동의")
    class AgreeTerms {

        @Nested
        @DisplayName("동의값을 전달하면")
        class WithAgreements {

            @Test
            @DisplayName("요청한 동의 항목을 반영한다")
            void appliesRequestedAgreements() {
                // given
                User user = User.builder().build();

                // when
                user.agreeTerms(true, true, true, false);

                // then
                assertThat(user.isPrivacyPolicyAgreed()).isTrue();
                assertThat(user.isTermsOfServiceAgreed()).isTrue();
                assertThat(user.isContentPolicyAgreed()).isTrue();
                assertThat(user.isMarketingConsent()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("이메일 인증")
    class VerifyEmail {

        @Test
        @DisplayName("인증하면 인증 완료 상태가 된다")
        void marksEmailAsVerified() {
            // given
            User user = User.builder().email_verified(false).build();

            // when
            user.markEmailVerified();

            // then
            assertThat(user.isEmail_verified()).isTrue();
        }
    }

    @Nested
    @DisplayName("프로필 변경")
    class UpdateProfile {

        @Test
        @DisplayName("닉네임을 변경하면 변경 시각을 갱신한다")
        void changesNickname() {
            // given
            LocalDateTime changedAt = LocalDateTime.of(2026, 8, 23, 12, 0);
            User user =
                    User.builder().nickname("기존 닉네임").profile_img("old-image").build();

            // when
            user.changeNickname("새 닉네임", changedAt);

            // then
            assertThat(user.getNickname()).isEqualTo("새 닉네임");
            assertThat(user.getProfile_img()).isEqualTo("old-image");
            assertThat(user.getUpdatedAt()).isEqualTo(changedAt);
        }

        @Test
        @DisplayName("프로필 이미지를 변경하면 변경 시각을 갱신한다")
        void changesProfileImage() {
            // given
            LocalDateTime changedAt = LocalDateTime.of(2026, 8, 23, 12, 0);
            User user =
                    User.builder().nickname("기존 닉네임").profile_img("old-image").build();

            // when
            user.changeProfileImage("new-image", changedAt);

            // then
            assertThat(user.getNickname()).isEqualTo("기존 닉네임");
            assertThat(user.getProfile_img()).isEqualTo("new-image");
            assertThat(user.getUpdatedAt()).isEqualTo(changedAt);
        }

        @Test
        @DisplayName("수정 요청을 기록하면 변경 시각을 갱신한다")
        void recordsProfileUpdate() {
            // given
            LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 23, 12, 0);
            User user = User.builder().build();

            // when
            user.recordProfileUpdate(updatedAt);

            // then
            assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    @Nested
    @DisplayName("탈퇴")
    class Withdraw {

        @Test
        @DisplayName("탈퇴 사유를 기록하면 탈퇴 상태가 된다")
        void recordsWithdrawalState() {
            // given
            LocalDateTime withdrawnAt = LocalDateTime.of(2026, 8, 23, 12, 0);
            User user = User.builder().build();

            // when
            user.withdraw(List.of(WithdrawalReason.NOT_USING, WithdrawalReason.OTHER), "직접 입력한 사유", withdrawnAt);

            // then
            assertThat(user.getDeletedAt()).isEqualTo(withdrawnAt);
            assertThat(user.getWithdrawalReason()).isEqualTo("NOT_USING,OTHER");
            assertThat(user.getWithdrawalOtherReason()).isEqualTo("직접 입력한 사유");
        }
    }

    @Nested
    @DisplayName("소셜 로그인 재가입")
    class ReactivateForSocialLogin {

        @Test
        @DisplayName("탈퇴 상태와 동의 이력을 초기화한다")
        void reactivatesAccount() {
            // given
            LocalDateTime withdrawnAt = LocalDateTime.of(2026, 8, 1, 12, 0);
            LocalDateTime lastUpdatedAt = LocalDateTime.of(2026, 8, 2, 12, 0);
            User user = User.builder()
                    .deletedAt(withdrawnAt)
                    .privacyPolicyAgreed(true)
                    .termsOfServiceAgreed(true)
                    .contentPolicyAgreed(true)
                    .marketingConsent(true)
                    .withdrawalReason("NOT_USING,OTHER")
                    .withdrawalOtherReason("직접 입력한 사유")
                    .updatedAt(lastUpdatedAt)
                    .build();

            // when
            user.reactivateForSocialLogin();

            // then
            assertThat(user.getDeletedAt()).isNull();
            assertThat(user.isPrivacyPolicyAgreed()).isFalse();
            assertThat(user.isTermsOfServiceAgreed()).isFalse();
            assertThat(user.isContentPolicyAgreed()).isFalse();
            assertThat(user.isMarketingConsent()).isFalse();
            assertThat(user.getWithdrawalReason()).isNull();
            assertThat(user.getWithdrawalOtherReason()).isNull();
            assertThat(user.getUpdatedAt()).isEqualTo(lastUpdatedAt);
        }
    }
}
