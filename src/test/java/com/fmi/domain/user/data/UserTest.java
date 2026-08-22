package com.fmi.domain.user.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void 임시_비밀번호를_재발급해도_최초의_원래_비밀번호를_보존한다() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 23, 12, 0);
        User user = User.builder().password("encoded-original-password").build();

        user.issueTemporaryPassword("encoded-first-temporary-password", now.plusHours(1), now);

        LocalDateTime reissuedAt = now.plusMinutes(10);
        LocalDateTime reissuedExpiresAt = reissuedAt.plusHours(1);
        user.issueTemporaryPassword("encoded-second-temporary-password", reissuedExpiresAt, reissuedAt);

        assertThat(user.getOriginalPassword()).isEqualTo("encoded-original-password");
        assertThat(user.getTemporaryPassword()).isEqualTo("encoded-second-temporary-password");
        assertThat(user.getTemporaryPasswordExpiresAt()).isEqualTo(reissuedExpiresAt);
        assertThat(user.getPassword()).isEqualTo("encoded-second-temporary-password");
        assertThat(user.getUpdatedAt()).isEqualTo(reissuedAt);
    }

    @Test
    void 임시_비밀번호가_만료되면_원래_비밀번호를_복원하고_임시_상태를_제거한다() {
        LocalDateTime issuedAt = LocalDateTime.of(2026, 8, 23, 12, 0);
        LocalDateTime expiresAt = issuedAt.plusHours(1);
        User user = User.builder().password("encoded-original-password").build();
        user.issueTemporaryPassword("encoded-temporary-password", expiresAt, issuedAt);

        boolean restored = user.restoreExpiredTemporaryPassword(expiresAt);

        assertThat(restored).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded-original-password");
        assertThat(user.getOriginalPassword()).isNull();
        assertThat(user.getTemporaryPassword()).isNull();
        assertThat(user.getTemporaryPasswordExpiresAt()).isNull();
        assertThat(user.getUpdatedAt()).isEqualTo(expiresAt);
    }

    @Test
    void 비밀번호를_변경하면_임시_비밀번호_상태를_모두_제거한다() {
        LocalDateTime issuedAt = LocalDateTime.of(2026, 8, 23, 12, 0);
        User user = User.builder().password("encoded-original-password").build();
        user.issueTemporaryPassword("encoded-temporary-password", issuedAt.plusHours(1), issuedAt);

        LocalDateTime changedAt = issuedAt.plusMinutes(10);
        user.changePassword("encoded-new-password", changedAt);

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(user.getOriginalPassword()).isNull();
        assertThat(user.getTemporaryPassword()).isNull();
        assertThat(user.getTemporaryPasswordExpiresAt()).isNull();
        assertThat(user.getUpdatedAt()).isEqualTo(changedAt);
    }
}
