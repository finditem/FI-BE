package com.fmi.service;

import com.fmi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhoneVerificationService {

    private final StringRedisTemplate redis;
    private final UserRepository userRepository;
    private final SmsService smsService;

    private static final SecureRandom RANDOM = new SecureRandom();

    private String redisKey(String phone) {
        return "phone:verify:" + phone;
    }

    @Transactional
    public void sendCode(String phone) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        // TTL 5분
        redis.opsForValue().set(redisKey(phone), code, Duration.ofMinutes(5));
        smsService.send(phone, "인증번호: " + code + " (5분 유효)");
    }

    @Transactional
    public boolean verifyAndMark(String phone, String code) {
        String cached = redis.opsForValue().get(redisKey(phone));
        if (cached == null || !cached.equals(code)) {
            return false;
        }
        // 사용 즉시 폐기
        redis.delete(redisKey(phone));

        // 가입 이전일 수도 있으므로: 있으면 바로 마킹, 없으면 가입 시 사용하도록 플래그 저장(30분 유효)
        userRepository.findByPhoneNumber(phone).ifPresentOrElse(u -> {
            u.setPhone_verified(true);
        }, () -> {
            redis.opsForValue().set("phone:verified:" + phone, "true", java.time.Duration.ofMinutes(30));
        });
        return true;
    }
}


