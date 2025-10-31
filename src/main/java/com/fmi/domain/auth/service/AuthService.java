package com.fmi.domain.auth.service;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.domain.auth.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redis;
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
        
        // 휴대폰 인증 선행 여부를 Redis 플래그로 확인
        boolean preVerified = false;
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            String flag = redis.opsForValue().get("phone:verified:" + request.getPhoneNumber());
            preVerified = "true".equals(flag);
            if (preVerified) {
                redis.delete("phone:verified:" + request.getPhoneNumber());
            }
        }

        User user = AuthConverter.toUserEntity(
                request, 
                passwordEncoder.encode(request.getPassword()), 
                preVerified
        );
        return userRepository.save(user).getId();
    }

    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._UNAUTHORIZED));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        return user;
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


