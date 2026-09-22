package com.fmi.domain.auth.service;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.admin.web.dto.AdminSignupRequest;
import com.fmi.domain.auth.data.RejoinType;
import com.fmi.domain.auth.event.UserSignedUpEvent;
import com.fmi.domain.auth.service.internal.PasswordValidator;
import com.fmi.domain.auth.service.internal.RejoinPolicy;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.user.service.internal.NicknameValidator;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Optional;
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
    private final RejoinPolicy rejoinPolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User signup(SignupRequest request) {
        Optional<User> existingUser = findRejoinUser(request.getEmail());
        passwordValidator.validateNewPassword(request.getPassword());
        nicknameValidator.validateAvailable(request.getNickname());

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        boolean isEmailVerified = signupEmailVerificationService.isEmailVerified(request.getEmail());
        if (!isEmailVerified) {
            throw new GeneralException(ErrorStatus._EMAIL_NOT_VERIFIED);
        }

        User user = existingUser.orElseGet(() -> createUser(request.getEmail(), request.getNickname(), Role.USER));
        if (existingUser.isPresent()) {
            // 탈퇴 계정은 새 가입 정보로 초기화한 뒤 동일한 사용자 식별자를 유지한다.
            user.reactivateForSignup(
                    encodedPassword,
                    request.getNickname(),
                    Role.USER,
                    Boolean.TRUE.equals(request.getPrivacyPolicyAgreed()),
                    Boolean.TRUE.equals(request.getTermsOfServiceAgreed()),
                    Boolean.TRUE.equals(request.getContentPolicyAgreed()),
                    Boolean.TRUE.equals(request.getMarketingConsent()));
        } else {
            user.changePassword(encodedPassword, null);
            user.agreeTerms(
                    Boolean.TRUE.equals(request.getPrivacyPolicyAgreed()),
                    Boolean.TRUE.equals(request.getTermsOfServiceAgreed()),
                    Boolean.TRUE.equals(request.getContentPolicyAgreed()),
                    Boolean.TRUE.equals(request.getMarketingConsent()));
            user.markEmailVerified();
        }
        User savedUser = userRepository.save(user);
        eventPublisher.publishEvent(UserSignedUpEvent.from(savedUser));

        return savedUser;
    }

    @Transactional
    public Long adminSignup(AdminSignupRequest request) {
        Optional<User> existingUser = findRejoinUser(request.getEmail());
        passwordValidator.validateNewPassword(request.getPassword());
        nicknameValidator.validateAvailable(request.getNickname());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = existingUser.orElseGet(() -> createUser(request.getEmail(), request.getNickname(), Role.ADMIN));
        if (existingUser.isPresent()) {
            user.reactivateForSignup(encodedPassword, request.getNickname(), Role.ADMIN, false, false, false, false);
        } else {
            user.changePassword(encodedPassword, null);
        }
        user.setEmail_verified(Boolean.TRUE.equals(request.getEmailVerified()));
        return userRepository.save(user).getId();
    }

    private Optional<User> findRejoinUser(String email) {
        Optional<User> existingUser = userRepository.findByEmailIncludingDeleted(email);
        if (existingUser.isEmpty()) {
            return existingUser;
        }

        User user = existingUser.get();
        if (user.getDeletedAt() == null) {
            throw new GeneralException(ErrorStatus._EMAIL_DUPLICATED);
        }

        rejoinPolicy.validate(user, RejoinType.EMAIL);
        return existingUser;
    }

    private User createUser(String email, String nickname, Role role) {
        return User.builder()
                .email(email)
                .nickname(nickname)
                .role(role)
                .profile_img("")
                .build();
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
