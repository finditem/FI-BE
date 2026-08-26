package com.fmi.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.Enum.WithdrawalReason;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.auth.web.dto.AccountDeleteRequest;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.external.oauth.kakao.KakaoOAuthClient;
import com.fmi.global.service.S3Service;
import com.fmi.security.RefreshTokenStore;
import com.fmi.service.EmailService;
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

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private UserRepository userRepository;

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
    private KakaoOAuthClient kakaoOAuthClient;

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Nested
    @DisplayName("계정 탈퇴")
    class DeleteAccount {

        @Nested
        @DisplayName("카카오 연동 계정이 탈퇴하면")
        class WithKakaoAccount {

            @Test
            @DisplayName("외부 연결과 사용자 데이터를 정해진 순서로 처리한다")
            void processesAccountDeletionInOrder() {
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

                withdrawalService.delete(email, request);

                InOrder order = inOrder(
                        s3Service, kakaoOAuthClient, userRepository, refreshTokenStore, postService, emailService);
                order.verify(s3Service).delete(List.of(profileImage));
                order.verify(kakaoOAuthClient).unlinkUser("kakao-user-id");
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
