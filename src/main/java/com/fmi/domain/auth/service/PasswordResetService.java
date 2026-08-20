package com.fmi.domain.auth.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.EmailService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private final UserRepository userRepository;
    private final SocialAccountsRepository socialAccountsRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 임시 비밀번호 발급
     * - 원래 비밀번호를 originalPassword에 보관
     * - 임시 비밀번호를 temporaryPassword에 저장 (1시간 유효)
     * - password 필드는 임시 비밀번호로 변경하여 로그인 가능하게 함
     */
    @Transactional
    public void issueTemporaryPassword(String email) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        if (socialAccountsRepository.findByUser(user).isPresent()) {
            throw new GeneralException(ErrorStatus._SOCIAL_ACCOUNT);
        }

        String tempPassword = generateTempPassword();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1); // 1시간 후 만료

        // 원래 비밀번호 보관 (임시 비밀번호가 없는 경우에만)
        if (user.getOriginalPassword() == null) {
            user.setOriginalPassword(user.getPassword());
        }

        // 임시 비밀번호 저장
        user.setTemporaryPassword(passwordEncoder.encode(tempPassword));
        user.setTemporaryPasswordExpiresAt(expiresAt);

        // password 필드에 임시 비밀번호 설정 (로그인 가능하게 하기 위해)
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // HTML 템플릿 사용
        emailService.sendHtmlEmail(
                email, "임시 비밀번호 발급", "password-reset-email.html", java.util.Map.of("PASSWORD", tempPassword));
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%^&*";
        StringBuilder sb = new StringBuilder();
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
