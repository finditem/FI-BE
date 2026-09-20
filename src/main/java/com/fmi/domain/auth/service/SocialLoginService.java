package com.fmi.domain.auth.service;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.user.service.internal.NicknameGenerator;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
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

    public User login(SocialLoginCommand command) {
        Optional<SocialAccounts> existingAccount =
                socialAccountsRepository.findByProviderAndProviderIdWithUser(command.provider(), command.providerId());

        // 소셜 로그인 이미 가입 했다면
        if (existingAccount.isPresent()) {
            User existingUser = existingAccount.get().getUser();
            if (existingUser.getDeletedAt() != null) {
                reactivate(existingUser);
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

    // 탈퇴한 유저가 재로그인 했을 때
    private void reactivate(User existingUser) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        if (existingUser.getDeletedAt().isAfter(oneWeekAgo)) {
            throw new GeneralException(ErrorStatus._EMAIL_RECENTLY_DELETED);
        }
        existingUser.reactivateForSocialLogin();
        userRepository.save(existingUser);
    }
}
