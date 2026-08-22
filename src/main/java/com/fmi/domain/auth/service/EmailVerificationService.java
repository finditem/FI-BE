package com.fmi.domain.auth.service;

import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.EmailBounceHandler;
import com.fmi.service.EmailService;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private final StringRedisTemplate redis;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final EmailBounceHandler emailBounceHandler;

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

        // Bounce back을 받은 이메일 주소인지 확인
        if (emailBounceHandler.hasBounced(email)) {
            log.warn("[EMAIL SEND BLOCKED] email={}, reason=이전에 bounce back을 받은 이메일 주소", email);
            throw new GeneralException(ErrorStatus._EMAIL_SEND_FAILED);
        }

        // 기존 인증 코드가 있다면 삭제 (새로운 코드 발송 시 이전 코드 무효화)
        redis.delete(key(email));

        // 중복이 아니면 인증번호 발송
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
        // 코드와 만료 시간을 함께 저장 (만료 시간을 명시적으로 체크하기 위해)
        String value = code + ":" + expiresAt.getEpochSecond();

        // Redis에 인증 코드 저장 (5분 TTL)
        redis.opsForValue().set(key(email), value, Duration.ofMinutes(5));

        // 이메일 비동기 발송 (SMTP 응답 대기 없이 즉시 API 응답)
        emailService.sendHtmlEmailAsync(email, "이메일 인증 코드", "verify-code.html", java.util.Map.of("CODE", code));
    }

    @Transactional
    public void verify(String email, String code) {
        String cached = redis.opsForValue().get(key(email));
        if (cached == null) {
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
        }

        // 코드와 만료 시간 분리
        String[] parts = cached.split(":");
        if (parts.length != 2) {
            // 이전 형식 호환성 (코드만 저장된 경우) - 만료 시간 정보가 없으므로 무효화
            // 보안상 이전 형식의 데이터는 만료된 것으로 간주
            redis.delete(key(email));
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
        }

        String storedCode = parts[0];
        long expiresAtSeconds = Long.parseLong(parts[1]);

        // 코드가 일치하지 않으면 실패
        if (!storedCode.equals(code)) {
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
        }

        // 만료 시간 체크
        Instant expiresAt = Instant.ofEpochSecond(expiresAtSeconds);
        if (Instant.now().isAfter(expiresAt)) {
            // 만료된 코드는 삭제
            redis.delete(key(email));
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
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
