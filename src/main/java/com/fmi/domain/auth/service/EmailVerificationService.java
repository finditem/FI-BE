package com.fmi.domain.auth.service;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private final StringRedisTemplate redis;
    private final EmailService emailService;
    private final UserRepository userRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    private String key(String email) {
        return "email:verify:" + email;
    }

    @Transactional
    public void sendCode(String email) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorStatus._EMAIL_DUPLICATED);
        }
        
        // 중복이 아니면 인증번호 발송
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redis.opsForValue().set(key(email), code, Duration.ofMinutes(5));
        emailService.sendEmail(email, "Your verification code", "인증번호: " + code + " (5분 유효)");
    }

    @Transactional
    public boolean verify(String email, String code) {
        String cached = redis.opsForValue().get(key(email));
        if (cached == null || !cached.equals(code)) {
            return false;
        }
        // 사용 즉시 폐기
        redis.delete(key(email));

        // 이메일 인증 완료 플래그를 Redis에 저장 (회원가입 시 검증용)
        String verifiedKey = "email:verified:" + email;
        redis.opsForValue().set(verifiedKey, "true", Duration.ofHours(24)); // 24시간 유효

        // 이미 존재하는 사용자의 경우 이메일 인증 상태 업데이트
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setEmail_verified(true);
        });
        return true;
    }

    /**
     * 이메일 인증 완료 여부 확인 (회원가입 시 사용)
     * Redis에 저장된 인증 완료 플래그를 확인합니다.
     */
    public boolean isEmailVerified(String email) {
        String verifiedKey = "email:verified:" + email;
        String verified = redis.opsForValue().get(verifiedKey);
        return "true".equals(verified);
    }

    /**
     * 회원가입 완료 후 인증 플래그 삭제 (한 번만 사용되도록)
     */
    @Transactional
    public void consumeEmailVerification(String email) {
        String verifiedKey = "email:verified:" + email;
        redis.delete(verifiedKey);
    }
}


