package com.fmi.domain.auth.service;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
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
        
        // 기존 인증 코드가 있다면 삭제 (새로운 코드 발송 시 이전 코드 무효화)
        redis.delete(key(email));
        
        // 중복이 아니면 인증번호 발송
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
        // 코드와 만료 시간을 함께 저장 (만료 시간을 명시적으로 체크하기 위해)
        String value = code + ":" + expiresAt.getEpochSecond();
        
        // 이메일 발송을 먼저 시도 (실패 시 예외 발생)
        try {
            emailService.sendEmail(email, "Your verification code", "인증번호: " + code + " (5분 유효)");
            // 이메일 발송 성공 시에만 Redis에 코드 저장
            redis.opsForValue().set(key(email), value, Duration.ofMinutes(5));
        } catch (Exception e) {
            // 이메일 발송 실패 시 Redis에 저장하지 않음 (이미 삭제했으므로 문제없음)
            // 예외를 다시 던져서 API가 실패 응답을 반환하도록 함
            throw e;
        }
    }

    @Transactional
    public void verify(String email, String code) {
        String cached = redis.opsForValue().get(key(email));
        if (cached == null) {
            log.warn("❌ [EMAIL VERIFY FAILED] email={}, reason=코드가 존재하지 않음 (TTL 만료 또는 삭제됨)", email);
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
        }
        
        // 코드와 만료 시간 분리
        String[] parts = cached.split(":");
        if (parts.length != 2) {
            // 이전 형식 호환성 (코드만 저장된 경우) - 만료 시간 정보가 없으므로 무효화
            // 보안상 이전 형식의 데이터는 만료된 것으로 간주
            log.warn("❌ [EMAIL VERIFY FAILED] email={}, reason=이전 형식 데이터 (만료 시간 정보 없음)", email);
            redis.delete(key(email));
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
        }
        
        String storedCode = parts[0];
        long expiresAtSeconds = Long.parseLong(parts[1]);
        
        // 코드가 일치하지 않으면 실패
        if (!storedCode.equals(code)) {
            log.warn("❌ [EMAIL VERIFY FAILED] email={}, reason=코드 불일치 (입력: {}, 저장: {})", email, code, storedCode);
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
        }
        
        // 만료 시간 체크
        Instant expiresAt = Instant.ofEpochSecond(expiresAtSeconds);
        if (Instant.now().isAfter(expiresAt)) {
            // 만료된 코드는 삭제
            log.warn("❌ [EMAIL VERIFY FAILED] email={}, reason=만료된 코드 (만료시간: {}, 현재시간: {})", email, expiresAt, Instant.now());
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
        
        log.info("✅ [EMAIL VERIFY SUCCESS] email={}", email);
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


