package com.fmi.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.fmi.domain.auth.web.dto.PasswordChangeRequest;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.security.RefreshTokenStore;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private PasswordService passwordService;

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Nested
        @DisplayName("임시 비밀번호 상태의 사용자이면")
        class WithTemporaryPassword {

            @Test
            @DisplayName("임시 상태를 지우고 모든 토큰을 폐기한다")
            void clearsTemporaryStateAndRevokesAllTokens() {
                String email = "member@finditem.kr";
                User user = User.builder()
                        .email(email)
                        .password("temporary-password-hash")
                        .originalPassword("original-password-hash")
                        .temporaryPassword("temporary-password-hash")
                        .temporaryPasswordExpiresAt(LocalDateTime.now().plusHours(1))
                        .build();
                PasswordChangeRequest request = new PasswordChangeRequest();
                request.setNewPassword("NewPassword1!");
                request.setNewPasswordConfirm("NewPassword1!");
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(passwordEncoder.encode("NewPassword1!")).thenReturn("new-password-hash");

                passwordService.change(email, request);

                InOrder order = inOrder(userRepository, refreshTokenStore);
                order.verify(userRepository).save(user);
                order.verify(refreshTokenStore).revokeAllForUser(email);
                assertThat(user.getPassword()).isEqualTo("new-password-hash");
                assertThat(user.getOriginalPassword()).isNull();
                assertThat(user.getTemporaryPassword()).isNull();
                assertThat(user.getTemporaryPasswordExpiresAt()).isNull();
            }
        }
    }
}
