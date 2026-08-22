package com.fmi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.Enum.WithdrawalReason;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.auth.service.KakaoOAuthService;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.user.web.dto.AccountDeleteRequest;
import com.fmi.domain.user.web.dto.PasswordChangeRequest;
import com.fmi.domain.user.web.dto.TermsAgreeRequest;
import com.fmi.global.service.S3Service;
import com.fmi.security.RefreshTokenStore;
import com.fmi.service.EmailService;
import java.time.LocalDateTime;
import java.util.List;
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
class UserServiceAuthTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private S3Service s3Service;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private PostService postService;

    @Mock
    private SocialAccountsRepository socialAccountsRepository;

    @Mock
    private KakaoOAuthService kakaoOAuthService;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Nested
        @DisplayName("임시 비밀번호 상태의 사용자이면")
        class WithTemporaryPassword {

            @Test
            @DisplayName("별도 검증 이력 없이 임시 상태를 지우고 모든 세션을 폐기한다")
            void clearsTemporaryStateAndRevokesAllSessions() {
                // given
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

                // when
                userService.changePassword(email, request);

                // then
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

    @Nested
    @DisplayName("약관 동의")
    class AgreeTerms {

        @Nested
        @DisplayName("사용자가 약관 동의를 요청하면")
        class WithTermsAgreementRequest {

            @Test
            @DisplayName("네 가지 동의 값을 사용자에게 저장한다")
            void storesAllFourAgreementValues() {
                // given
                String email = "member@finditem.kr";
                User user = User.builder().email(email).build();
                TermsAgreeRequest request = new TermsAgreeRequest();
                request.setPrivacyPolicyAgreed(true);
                request.setTermsOfServiceAgreed(true);
                request.setContentPolicyAgreed(true);
                request.setMarketingConsent(false);
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

                // when
                userService.agreeTerms(email, request);

                // then
                verify(userRepository).save(user);
                assertThat(user.isPrivacyPolicyAgreed()).isTrue();
                assertThat(user.isTermsOfServiceAgreed()).isTrue();
                assertThat(user.isContentPolicyAgreed()).isTrue();
                assertThat(user.isMarketingConsent()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("계정 탈퇴")
    class DeleteAccount {

        @Nested
        @DisplayName("카카오 연동 계정이 탈퇴하면")
        class WithKakaoAccount {

            @Test
            @DisplayName("S3, 카카오, 저장, 세션, 게시글, 메일 순서로 처리한다")
            void processesAccountDeletionInOrder() {
                // given
                String email = "member@finditem.kr";
                String profileImage = "https://bucket.example.com/profile.png";
                User user = User.builder()
                        .id(1L)
                        .email(email)
                        .nickname("찾아줘토끼")
                        .profile_img(profileImage)
                        .build();
                SocialAccounts kakaoAccount = SocialAccounts.builder()
                        .provider(Provider.KAKAO)
                        .providerId("kakao-user-id")
                        .user(user)
                        .build();
                AccountDeleteRequest request = new AccountDeleteRequest();
                request.setReasons(List.of(WithdrawalReason.NOT_USING));
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(s3Service.isValidS3Url(profileImage)).thenReturn(true);
                when(socialAccountsRepository.findByUser(user)).thenReturn(Optional.of(kakaoAccount));

                // when
                userService.deleteAccount(email, request);

                // then
                InOrder order = inOrder(
                        s3Service, kakaoOAuthService, userRepository, refreshTokenStore, postService, emailService);
                order.verify(s3Service).delete(List.of(profileImage));
                order.verify(kakaoOAuthService).unlinkUser("kakao-user-id");
                order.verify(userRepository).save(user);
                order.verify(refreshTokenStore).revokeAllForUser(email);
                order.verify(postService).softDeleteAllByUser(user);
                order.verify(emailService)
                        .sendHtmlEmailAsync(eq(email), eq("계정이 삭제되었습니다"), eq("account-deletion-email.html"), anyMap());
                assertThat(user.getDeletedAt()).isNotNull();
                assertThat(user.getWithdrawalReason()).isEqualTo("NOT_USING");
            }
        }
    }
}
