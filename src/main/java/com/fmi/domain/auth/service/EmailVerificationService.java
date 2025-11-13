package com.fmi.domain.auth.service;

import com.fmi.domain.auth.data.User;
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

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        user.setEmail_verified(true);
        return true;
    }
}


