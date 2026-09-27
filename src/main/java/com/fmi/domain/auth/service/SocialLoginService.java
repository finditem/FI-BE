package com.fmi.domain.auth.service;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.data.RejoinType;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.data.SocialLoginCommand;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.auth.service.internal.RejoinPolicy;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.user.service.internal.NicknameGenerator;
import java.util.Locale;
import java.util.Optional;
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
    private final NicknameGenerator nicknameGenerator;
    private final RejoinPolicy rejoinPolicy;

    public User login(SocialLoginCommand command) {
        Optional<SocialAccounts> existingAccount =
                socialAccountsRepository.findByProviderAndProviderIdWithUser(command.provider(), command.providerId());

        // 소셜 로그인 이미 가입 했다면
        if (existingAccount.isPresent()) {
            User existingUser = existingAccount.get().getUser();
            if (existingUser.getDeletedAt() != null) {
                // 탈퇴 계정은 재가입 정책을 검증한 뒤 탈퇴 상태와 약관 동의 이력을 초기화한다.
                rejoinPolicy.validate(existingUser, rejoinType(command));
                existingUser.reactivateForSocialLogin();
                userRepository.save(existingUser);
            }
            return existingUser;
        }

        // 소셜 로그인 가입 하지 않았다면
        String providerName = command.provider().name().toLowerCase(Locale.ROOT);
        String internalEmail = providerName + "_" + command.providerId() + "@" + providerName + ".local";
        String nickname = command.nickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = nicknameGenerator.generate();
        }
        String profileImageUrl = command.profileImageUrl();
        if (profileImageUrl == null) {
            profileImageUrl = "";
        }
        String encodedPassword = passwordEncoder.encode("{noop}-" + command.providerId());
        User user = AuthConverter.toSocialUserEntity(internalEmail, nickname, profileImageUrl, encodedPassword);
        User savedUser = userRepository.save(user);

        SocialAccounts account = SocialAccounts.builder()
                .user(savedUser)
                .provider(command.provider())
                .providerId(command.providerId())
                .build();
        socialAccountsRepository.save(account);

        return savedUser;
    }

    private RejoinType rejoinType(SocialLoginCommand command) {
        return switch (command.provider()) {
            case KAKAO -> RejoinType.KAKAO;
            default -> RejoinType.SOCIAL;
        };
    }
}
