package com.fmi.domain.auth.service;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.admin.web.dto.AdminSignupRequest;
import com.fmi.domain.auth.event.UserSignedUpEvent;
import com.fmi.domain.auth.service.internal.PasswordValidator;
import com.fmi.domain.auth.service.internal.SignupValidator;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.user.service.internal.NicknameValidator;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NicknameValidator nicknameValidator;
    private final SignupEmailVerificationService signupEmailVerificationService;
    private final PasswordValidator passwordValidator;
    private final SignupValidator signupValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User signup(SignupRequest request) {
        signupValidator.validate(request.getEmail());
        passwordValidator.validateNewPassword(request.getPassword());
        nicknameValidator.validateAvailable(request.getNickname());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .role(Role.USER)
                .email_verified(true)
                .privacyPolicyAgreed(Boolean.TRUE.equals(request.getPrivacyPolicyAgreed()))
                .termsOfServiceAgreed(Boolean.TRUE.equals(request.getTermsOfServiceAgreed()))
                .contentPolicyAgreed(Boolean.TRUE.equals(request.getContentPolicyAgreed()))
                .marketingConsent(Boolean.TRUE.equals(request.getMarketingConsent()))
                .build();

        boolean isEmailVerified = signupEmailVerificationService.isEmailVerified(request.getEmail());
        if (!isEmailVerified) {
            throw new GeneralException(ErrorStatus._EMAIL_NOT_VERIFIED);
        }

        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(UserSignedUpEvent.from(savedUser));

        return savedUser;
    }

    @Transactional
    public Long adminSignup(AdminSignupRequest request) {
        signupValidator.validate(request.getEmail());
        passwordValidator.validateNewPassword(request.getPassword());
        nicknameValidator.validateAvailable(request.getNickname());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .role(Role.ADMIN)
                .email_verified(Boolean.TRUE.equals(request.getEmailVerified()))
                .privacyPolicyAgreed(false)
                .termsOfServiceAgreed(false)
                .contentPolicyAgreed(false)
                .marketingConsent(false)
                .build();
        return userRepository.save(user).getId();
    }

    public AuthenticateResult authenticate(String email, String rawPassword) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INVALID_CREDENTIALS));

        boolean isTemporaryPassword = passwordValidator.matchesTemporaryPassword(user, rawPassword);
        if (!isTemporaryPassword && !passwordValidator.matchesPermanentPassword(user, rawPassword)) {
            throw new GeneralException(ErrorStatus._INVALID_CREDENTIALS);
        }
        return new AuthenticateResult(user, isTemporaryPassword);
    }

    @Data
    public static class AuthenticateResult {
        private final User user;
        private final boolean isTemporaryPassword;
    }
}
