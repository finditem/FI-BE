package com.fmi.domain.auth.service;

import com.fmi.domain.admin.web.dto.AdminSignupRequest;
import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.EmailService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NicknameValidationService nicknameValidationService;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;

    @Transactional
    public User signup(SignupRequest request) {
        // 활성 사용자 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new GeneralException(ErrorStatus._EMAIL_DUPLICATED);
        }

        // 일주일(7일) 이내 탈퇴한 이메일 재가입 방지
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        if (userRepository.existsRecentlyDeletedByEmail(request.getEmail(), oneWeekAgo)) {
            throw new GeneralException(ErrorStatus._EMAIL_RECENTLY_DELETED);
        }

        // 비밀번호 규칙: 8자 이상, 대문자/소문자/숫자/특수문자 포함
        String pw = request.getPassword() == null ? "" : request.getPassword();
        boolean valid = pw.length() >= 8
                && pw.length() <= 16
                && pw.matches(".*[A-Z].*")
                && pw.matches(".*[a-z].*")
                && pw.matches(".*[0-9].*")
                && pw.matches(".*[!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?].*");
        if (!valid) {
            throw new GeneralException(ErrorStatus._WEAK_PASSWORD);
        }

        // 약관 동의 필드는 @NotNull로 값은 필수지만, true/false 모두 허용
        // (null 체크는 @NotNull에서 처리됨)

        // 보안: 프론트엔드에서 보낸 emailVerified 값은 무시하고, Redis에서 실제 인증 여부 확인
        boolean isEmailVerified = emailVerificationService.isEmailVerified(request.getEmail());
        if (!isEmailVerified) {
            throw new GeneralException(ErrorStatus._EMAIL_NOT_VERIFIED);
        }

        // 인증 플래그 소비 (한 번만 사용되도록)
        emailVerificationService.consumeEmailVerification(request.getEmail());

        User user = AuthConverter.toUserEntity(
                request, passwordEncoder.encode(request.getPassword()), true // 백엔드에서 검증한 인증 여부 사용
                );
        User savedUser = userRepository.save(user);

        // 회원가입 환영 이메일 발송 (비동기로 처리하거나 트랜잭션 외부에서 처리 권장)
        try {
            String nickname = savedUser.getNickname() != null ? savedUser.getNickname() : "회원";
            String email = savedUser.getEmail();
            String signupDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                    .format(savedUser.getCreatedAt() != null ? savedUser.getCreatedAt() : LocalDateTime.now());

            emailService.sendHtmlEmailAsync(
                    email,
                    "회원가입을 환영합니다",
                    "welcome-email.html",
                    java.util.Map.of(
                            "NAME", nickname,
                            "USER", email,
                            "DATE", signupDate));
        } catch (Exception e) {
            log.warn("회원가입 환영 이메일 발송 실패: email={}", savedUser.getEmail(), e);
        }

        return savedUser;
    }

    /**
     * 관리자 회원가입
     * Role을 ADMIN으로 강제 설정합니다.
     * 동의 항목은 받지 않습니다.
     */
    @Transactional
    public Long adminSignup(AdminSignupRequest request) {
        // 활성 사용자 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new GeneralException(ErrorStatus._EMAIL_DUPLICATED);
        }

        // 일주일(7일) 이내 탈퇴한 이메일 재가입 방지
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        if (userRepository.existsRecentlyDeletedByEmail(request.getEmail(), oneWeekAgo)) {
            throw new GeneralException(ErrorStatus._EMAIL_RECENTLY_DELETED);
        }

        // 비밀번호 규칙: 8자 이상, 대문자/소문자/숫자/특수문자 포함
        String pw = request.getPassword() == null ? "" : request.getPassword();
        boolean valid = pw.length() >= 8
                && pw.length() <= 16
                && pw.matches(".*[A-Z].*")
                && pw.matches(".*[a-z].*")
                && pw.matches(".*[0-9].*")
                && pw.matches(".*[!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?].*");
        if (!valid) {
            throw new GeneralException(ErrorStatus._WEAK_PASSWORD);
        }

        // Role을 ADMIN으로 강제 설정
        User user = AuthConverter.toAdminUserEntity(request, passwordEncoder.encode(request.getPassword()));
        return userRepository.save(user).getId();
    }

    /**
     * 인증 및 임시 비밀번호 여부 확인
     * @return AuthenticateResult (user, isTemporaryPassword)
     */
    public AuthenticateResult authenticate(String email, String rawPassword) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INVALID_CREDENTIALS));

        boolean isTemporaryPassword = false;
        boolean passwordMatches = false;

        // 임시 비밀번호가 활성화되어 있는지 확인
        boolean hasActiveTemporaryPassword = user.getTemporaryPassword() != null
                && user.getTemporaryPasswordExpiresAt() != null
                && LocalDateTime.now().isBefore(user.getTemporaryPasswordExpiresAt());

        // 임시 비밀번호가 활성화되어 있으면 원래 비밀번호와 임시 비밀번호 모두 확인
        if (hasActiveTemporaryPassword) {
            // 임시 비밀번호 확인 (우선 확인)
            if (passwordEncoder.matches(rawPassword, user.getTemporaryPassword())) {
                passwordMatches = true;
                isTemporaryPassword = true;
            }
            // 원래 비밀번호 확인
            else if (user.getOriginalPassword() != null
                    && passwordEncoder.matches(rawPassword, user.getOriginalPassword())) {
                passwordMatches = true;
                isTemporaryPassword = false;
            }
        }
        // 임시 비밀번호가 없으면 일반 비밀번호만 확인
        else {
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                passwordMatches = true;
                isTemporaryPassword = false;
            }
        }

        if (!passwordMatches) {
            throw new GeneralException(ErrorStatus._INVALID_CREDENTIALS);
        }

        return new AuthenticateResult(user, isTemporaryPassword);
    }

    /**
     * 활성 사용자 조회 (deletedAt이 null인 사용자만)
     */
    public Optional<User> findActiveUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Data
    public static class AuthenticateResult {
        private final User user;
        private final boolean isTemporaryPassword;
    }

    /**
     * 닉네임 유효성 및 중복 검사
     * @return NicknameCheckResult (available, errorType, message)
     */
    public NicknameCheckResult checkNickname(String nickname) {
        // 1단계: 유효성 검증 (길이, 금칙어)
        NicknameValidationService.ValidationResult validationResult = nicknameValidationService.validate(nickname);
        if (!validationResult.isValid()) {
            return NicknameCheckResult.invalid("부적절한 닉네임입니다");
        }

        // 2단계: 중복 검사
        if (userRepository.existsByNickname(nickname.trim())) {
            return NicknameCheckResult.duplicate("중복된 닉네임입니다");
        }

        return NicknameCheckResult.available();
    }

    @Data
    public static class NicknameCheckResult {
        private final boolean available;
        private final String errorType; // "INVALID" or "DUPLICATE"
        private final String message;

        private NicknameCheckResult(boolean available, String errorType, String message) {
            this.available = available;
            this.errorType = errorType;
            this.message = message;
        }

        public static NicknameCheckResult available() {
            return new NicknameCheckResult(true, null, null);
        }

        public static NicknameCheckResult invalid(String message) {
            return new NicknameCheckResult(false, "INVALID", message);
        }

        public static NicknameCheckResult duplicate(String message) {
            return new NicknameCheckResult(false, "DUPLICATE", message);
        }
    }
}
