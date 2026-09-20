package com.fmi.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.user.service.internal.NicknameGenerator;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @Mock
    private SocialAccountsRepository socialAccountsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Nested
    @DisplayName("최초 소셜 로그인")
    class FirstSocialLogin {

        @ParameterizedTest
        @CsvSource({"KAKAO, 123456, kakao_123456@kakao.local", "APPLE, 000123, apple_000123@apple.local"})
        @DisplayName("제공자별 내부 이메일로 사용자와 소셜 계정을 생성한다")
        void createsUserAndSocialAccount(Provider provider, String providerId, String expectedEmail) {
            // given
            SocialLoginCommand command = new SocialLoginCommand(provider, providerId, "찾아줘토끼1", null);
            when(socialAccountsRepository.findByProviderAndProviderIdWithUser(provider, providerId))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode("{noop}-" + providerId)).thenReturn("encoded-password");
            사용자_저장과_조회가_성공한다();

            // when
            User result = socialLoginService.login(command);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            ArgumentCaptor<SocialAccounts> accountCaptor = ArgumentCaptor.forClass(SocialAccounts.class);
            verify(userRepository).save(userCaptor.capture());
            verify(socialAccountsRepository).save(accountCaptor.capture());
            verify(userRepository, never()).findByEmail(anyString());

            User createdUser = userCaptor.getValue();
            SocialAccounts createdAccount = accountCaptor.getValue();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isSameAs(createdUser);
                softly.assertThat(createdUser.getEmail()).isEqualTo(expectedEmail);
                softly.assertThat(createdUser.getNickname()).isEqualTo("찾아줘토끼1");
                softly.assertThat(createdUser.getProfile_img()).isEmpty();
                softly.assertThat(createdUser.getRole()).isEqualTo(Role.USER);
                softly.assertThat(createdAccount.getUser()).isSameAs(createdUser);
                softly.assertThat(createdAccount.getProvider()).isEqualTo(provider);
                softly.assertThat(createdAccount.getProviderId()).isEqualTo(providerId);
            });
        }

        @Test
        @DisplayName("제공자 닉네임이 없으면 닉네임을 생성한다")
        void generatesNicknameWhenProviderNicknameIsMissing() {
            // given
            SocialLoginCommand command = new SocialLoginCommand(Provider.APPLE, "apple-subject", null, null);
            when(socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.APPLE, "apple-subject"))
                    .thenReturn(Optional.empty());
            when(nicknameGenerator.generate()).thenReturn("찾아줘토끼1");
            when(passwordEncoder.encode("{noop}-apple-subject")).thenReturn("encoded-password");
            사용자_저장과_조회가_성공한다();

            // when
            socialLoginService.login(command);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getNickname()).isEqualTo("찾아줘토끼1");
        }
    }

    @Nested
    @DisplayName("기존 소셜 로그인")
    class ExistingSocialLogin {

        @ParameterizedTest
        @CsvSource({"KAKAO, 123456", "APPLE, apple-subject"})
        @DisplayName("같은 제공자와 계정 식별자로 로그인하면 기존 사용자를 반환한다")
        void returnsExistingUser(Provider provider, String providerId) {
            // given
            User existingUser =
                    User.builder().id(1L).email("internal@provider.local").build();
            SocialAccounts existingAccount = SocialAccounts.builder()
                    .user(existingUser)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            SocialLoginCommand command = new SocialLoginCommand(provider, providerId, null, null);
            when(socialAccountsRepository.findByProviderAndProviderIdWithUser(provider, providerId))
                    .thenReturn(Optional.of(existingAccount));

            // when
            User result = socialLoginService.login(command);

            // then
            assertThat(result).isSameAs(existingUser);
            verify(userRepository, never()).save(any(User.class));
            verify(socialAccountsRepository, never()).save(any(SocialAccounts.class));
        }
    }

    private void 사용자_저장과_조회가_성공한다() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });
        when(socialAccountsRepository.save(any(SocialAccounts.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
