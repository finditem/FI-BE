package com.fmi.service;

import com.fmi.domain.EmailVerification;
import com.fmi.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private final EmailVerificationRepository repository;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void sendCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailVerification ev = EmailVerification.builder()
                .email(email)
                .code(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        repository.save(ev);
        emailService.sendEmail(email, "Your verification code", "인증번호: " + code + " (5분 유효)");
    }

    public boolean verify(String email, String code) {
        return repository.findTopByEmailOrderByCreatedAtDesc(email)
                .filter(ev -> ev.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(ev -> ev.getCode().equals(code))
                .orElse(false);
    }
}


