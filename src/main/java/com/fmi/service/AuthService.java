package com.fmi.service;

import com.fmi.domain.User;
import com.fmi.domain.Enum.Role;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;

    @Transactional
    public Long signup(String email, String rawPassword, String nickname, String name,
                       String phoneNumber, String profileImg, Role role,
                       Boolean termsOfServiceAgreed, Boolean privacyPolicyAgreed, Boolean marketingConsent,
                       Long trustScore, Boolean emailVerified, Boolean phoneVerified) {
        if (userRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorStatus._EMAIL_DUPLICATED);
        }
        // 비밀번호 규칙: 8자 이상, 대문자/소문자/숫자/특수문자 포함
        String pw = rawPassword == null ? "" : rawPassword;
        boolean valid = pw.length() >= 8 && pw.length() <= 16
                && pw.matches(".*[A-Z].*")
                && pw.matches(".*[a-z].*")
                && pw.matches(".*[0-9].*")
                && pw.matches(".*[!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?].*");
        if (!valid) {
            throw new GeneralException(ErrorStatus._WEAK_PASSWORD);
        }
        // 휴대폰 인증 선행 여부를 Redis 플래그로 확인(있으면 true로 설정)
        boolean preVerified = false;
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            String flag = redis.opsForValue().get("phone:verified:" + phoneNumber);
            preVerified = "true".equals(flag);
            if (preVerified) {
                redis.delete("phone:verified:" + phoneNumber);
            }
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .name(name)
                .phoneNumber(phoneNumber)
                .profile_img(profileImg != null ? profileImg : "")
                .role(role != null ? role : Role.USER)
                .termsOfServiceAgreed(Boolean.TRUE.equals(termsOfServiceAgreed))
                .privacyPolicyAgreed(Boolean.TRUE.equals(privacyPolicyAgreed))
                .marketingConsent(Boolean.TRUE.equals(marketingConsent))
                .trust_score(trustScore != null ? trustScore : 0L)
                .email_verified(Boolean.TRUE.equals(emailVerified))
                .phone_verified(preVerified || Boolean.TRUE.equals(phoneVerified))
                .build();
        return userRepository.save(user).getUserId();
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

    public boolean nicknameExists(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}


