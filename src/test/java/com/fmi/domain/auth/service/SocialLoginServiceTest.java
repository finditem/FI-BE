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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    private User savedUser;

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

    private void 사용자_저장과_조회가_성공한다() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });
        when(socialAccountsRepository.save(any(SocialAccounts.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(1L)).thenAnswer(invocation -> Optional.ofNullable(savedUser));
    }

    @Test
    void 최초_Apple_로그인이면_사용자와_소셜_계정을_생성한다() {
        // given
        String subject = "apple-subject";
        when(socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.APPLE, subject))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("찾아줘토끼1");
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        사용자_저장과_조회가_성공한다();
        // when
        SocialLoginService.AppleLoginResult result = socialLoginService.upsertUserFromApple(subject);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<SocialAccounts> socialAccountCaptor = ArgumentCaptor.forClass(SocialAccounts.class);
        verify(userRepository).save(userCaptor.capture());
        verify(socialAccountsRepository).save(socialAccountCaptor.capture());

        User savedUser = userCaptor.getValue();
        SocialAccounts savedSocialAccount = socialAccountCaptor.getValue();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.user()).isSameAs(savedUser);
            softly.assertThat(savedUser.getEmail()).isEqualTo("apple_apple-subject@apple.local");
            softly.assertThat(savedUser.getNickname()).isEqualTo("찾아줘토끼1");
            softly.assertThat(savedUser.getRole()).isEqualTo(Role.USER);
            softly.assertThat(savedSocialAccount.getUser()).isSameAs(savedUser);
            softly.assertThat(savedSocialAccount.getProvider()).isEqualTo(Provider.APPLE);
            softly.assertThat(savedSocialAccount.getProviderId()).isEqualTo(subject);
        });
    }

    @Test
    void 같은_subject로_재로그인하면_기존_사용자를_반환한다() {
        // given
        String subject = "apple-subject";
        User existingUser =
                User.builder().id(1L).email("apple_apple-subject@apple.local").build();
        SocialAccounts existingAccount = SocialAccounts.builder()
                .user(existingUser)
                .provider(Provider.APPLE)
                .providerId(subject)
                .build();
        when(socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.APPLE, subject))
                .thenReturn(Optional.of(existingAccount));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        // when
        SocialLoginService.AppleLoginResult result = socialLoginService.upsertUserFromApple(subject);

        // then
        assertThat(result.user()).isSameAs(existingUser);
        verify(userRepository, never()).save(any(User.class));
        verify(socialAccountsRepository, never()).save(any(SocialAccounts.class));
    }

    @Test
    void Apple_로그인은_같은_이메일의_일반_계정과_별도_사용자를_생성한다() {
        // given
        String subject = "apple-subject";
        when(socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.APPLE, subject))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generate()).thenReturn("찾아줘토끼1");
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        사용자_저장과_조회가_성공한다();
        // when
        socialLoginService.upsertUserFromApple(subject);

        // then
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository).save(any(User.class));
    }
}
