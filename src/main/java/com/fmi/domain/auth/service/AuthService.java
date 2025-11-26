package com.fmi.domain.auth.service;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.domain.admin.web.dto.AdminSignupRequest;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.domain.auth.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NicknameValidationService nicknameValidationService;

    @Transactional
    public Long signup(SignupRequest request) {
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
        boolean valid = pw.length() >= 8 && pw.length() <= 16
                && pw.matches(".*[A-Z].*")
                && pw.matches(".*[a-z].*")
                && pw.matches(".*[0-9].*")
                && pw.matches(".*[!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?].*");
        if (!valid) {
            throw new GeneralException(ErrorStatus._WEAK_PASSWORD);
        }

        User user = AuthConverter.toUserEntity(
                request, 
                passwordEncoder.encode(request.getPassword())
        );
        return userRepository.save(user).getId();
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
        boolean valid = pw.length() >= 8 && pw.length() <= 16
                && pw.matches(".*[A-Z].*")
                && pw.matches(".*[a-z].*")
                && pw.matches(".*[0-9].*")
                && pw.matches(".*[!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?].*");
        if (!valid) {
            throw new GeneralException(ErrorStatus._WEAK_PASSWORD);
        }

        // Role을 ADMIN으로 강제 설정
        User user = AuthConverter.toAdminUserEntity(
                request, 
                passwordEncoder.encode(request.getPassword())
        );
        return userRepository.save(user).getId();
    }

    /**
     * 인증 및 임시 비밀번호 여부 확인
     * @return AuthenticateResult (user, isTemporaryPassword)
     */
    public AuthenticateResult authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._UNAUTHORIZED));
        
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
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        
        return new AuthenticateResult(user, isTemporaryPassword);
    }
    
    @Data
    public static class AuthenticateResult {
        private final User user;
        private final boolean isTemporaryPassword;
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
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


