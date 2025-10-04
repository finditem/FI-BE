package com.fmi.service;

import com.fmi.domain.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
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
    public boolean verifyAndMark(String phone, String code, String email) {
        String cached = redis.opsForValue().get(redisKey(phone));
        if (cached == null || !cached.equals(code)) {
            return false;
        }
        // 사용 즉시 폐기
        redis.delete(redisKey(phone));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        user.setPhone_verified(true);
        user.setPhone_number(phone);
        return true;
    }
}


