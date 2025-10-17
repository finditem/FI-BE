package com.fmi.domain.auth.service;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialLoginService {

    private final SocialAccountsRepository socialAccountsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User upsertUserFromKakao(Long kakaoId, String email, String nickname, String profileImageUrl) {
        String providerId = String.valueOf(kakaoId);

        return socialAccountsRepository.findByProviderAndProviderId(Provider.KAKAO, providerId)
                .map(SocialAccounts::getUser)
                .orElseGet(() -> {
                    // 이메일이 없으면 대체 이메일 생성
                    String effectiveEmail = (email != null && !email.isBlank())
                            ? email
                            : ("kakao_" + providerId + "@kakao.local");

                    // 사용자 존재 여부 확인(이메일 기준)
                    User user = userRepository.findByEmail(effectiveEmail)
                            .orElseGet(() -> {
                                User u = User.builder()
                                        .email(effectiveEmail)
                                        .password(passwordEncoder.encode("{noop}-" + providerId)) // 소셜계정: 사용 안 함
                                        .nickname(nickname != null ? nickname : ("kakao_" + providerId))
                                        .name(nickname != null ? nickname : ("kakao_" + providerId))
                                        .profile_img(profileImageUrl != null ? profileImageUrl : "")
                                        .role(Role.USER)
                                        .email_verified(true)
                                        .phone_verified(false)
                                        .termsOfServiceAgreed(false)
                                        .privacyPolicyAgreed(false)
                                        .marketingConsent(false)
                                        .trust_score(0L)
                                        .build();
                                return userRepository.save(u);
                            });

                    // 소셜 계정 연결 저장
                    SocialAccounts account = SocialAccounts.builder()
                            .user(user)
                            .provider(Provider.KAKAO)
                            .providerId(providerId)
                            .build();
                    socialAccountsRepository.save(account);

                    return user;
                });
    }
}


