package com.fmi.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemporaryPasswordCleanupScheduler")
class TemporaryPasswordCleanupSchedulerTest {

    @Mock
    private UserRepository userRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-23T03:00:00Z"), ZoneOffset.UTC);
    private TemporaryPasswordCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TemporaryPasswordCleanupScheduler(userRepository, clock);
    }

    @Nested
    @DisplayName("만료된 임시 비밀번호를 정리할 때")
    class CleanupExpiredTemporaryPasswords {

        @Nested
        @DisplayName("만료 사용자가 있으면")
        class WithExpiredTemporaryPasswordUsers {

            @Test
            @DisplayName("고정된 현재 시각으로 조회해 원래 비밀번호를 복원하고 저장한다")
            void restoresAndSavesExpiredTemporaryPasswordUsers() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 8, 23, 3, 0);
                User user = User.builder().password("original-password-hash").build();
                user.issueTemporaryPassword("temporary-password-hash", now, now.minusHours(1));
                when(userRepository.findUsersWithExpiredTemporaryPassword(now)).thenReturn(List.of(user));

                // when
                scheduler.cleanupExpiredTemporaryPasswords();

                // then
                verify(userRepository).findUsersWithExpiredTemporaryPassword(now);
                verify(userRepository).save(user);
                assertThat(user.getPassword()).isEqualTo("original-password-hash");
                assertThat(user.getOriginalPassword()).isNull();
                assertThat(user.getTemporaryPassword()).isNull();
                assertThat(user.getTemporaryPasswordExpiresAt()).isNull();
                assertThat(user.getUpdatedAt()).isEqualTo(now);
            }
        }

        @Nested
        @DisplayName("만료 사용자가 없으면")
        class WithoutExpiredTemporaryPasswordUsers {

            @Test
            @DisplayName("저장하지 않는다")
            void doesNotSaveWhenThereAreNoExpiredTemporaryPasswordUsers() {
                // given
                LocalDateTime now = LocalDateTime.of(2026, 8, 23, 3, 0);
                when(userRepository.findUsersWithExpiredTemporaryPassword(now)).thenReturn(List.of());

                // when
                scheduler.cleanupExpiredTemporaryPasswords();

                // then
                verify(userRepository).findUsersWithExpiredTemporaryPassword(now);
                verify(userRepository, never()).save(any(User.class));
            }
        }
    }
}
