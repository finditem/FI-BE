package com.fmi.domain.auth.service;

import com.fmi.domain.auth.service.internal.AuthEmailNotifier;
import com.fmi.domain.auth.service.internal.EmailVerificationStore;
import com.fmi.domain.auth.service.internal.EmailVerificationStore.VerificationCode;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.external.mail.EmailBounceRegistry;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SignupEmailVerificationService {

    private final EmailVerificationStore emailVerificationStore;
    private final AuthEmailNotifier authEmailNotifier;
    private final UserRepository userRepository;
    private final EmailBounceRegistry emailBounceRegistry;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void sendCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorStatus._EMAIL_DUPLICATED);
        }

        // Bounce back을 받은 이메일 주소인지 확인
        if (emailBounceRegistry.hasBounced(email)) {
            log.warn("[EMAIL SEND BLOCKED] email={}, reason=이전에 bounce back을 받은 이메일 주소", email);
            throw new GeneralException(ErrorStatus._EMAIL_SEND_FAILED);
        }

        // 중복이 아니면 인증번호 발송
        int randomValue = RANDOM.nextInt(1_000_000);
        String code = String.format("%06d", randomValue);
        Instant expiresAt = Instant.now().plusSeconds(300);
        emailVerificationStore.replaceCode(email, code, expiresAt);

        // 이메일 비동기 발송 (SMTP 응답 대기 없이 즉시 API 응답)
        authEmailNotifier.sendVerificationCode(email, code);
    }

    @Transactional
    public void verify(String email, String code) {
        Optional<VerificationCode> storedVerificationCode = emailVerificationStore.findCode(email);
        if (storedVerificationCode.isEmpty()) {
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
        }
        VerificationCode verificationCode = storedVerificationCode.get();

        // 코드가 일치하지 않으면 실패
        if (!verificationCode.value().equals(code)) {
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
        }

        // 만료 시간 체크
        if (Instant.now().isAfter(verificationCode.expiresAt())) {
            // 만료된 코드는 삭제
            emailVerificationStore.deleteCode(email);
            throw new GeneralException(ErrorStatus._EMAIL_VERIFY_FAILED);
        }

        // 사용 즉시 폐기
        emailVerificationStore.deleteCode(email);

        // 회원가입 시 확인할 이메일 인증 완료 상태 저장
        emailVerificationStore.markVerified(email);

        // 이미 존재하는 사용자의 경우 이메일 인증 상태 업데이트
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            user.get().markEmailVerified();
        }
    }

    public boolean isEmailVerified(String email) {
        return emailVerificationStore.isVerified(email);
    }

    public void consumeEmailVerification(String email) {
        emailVerificationStore.consumeVerification(email);
    }

    public void registerBounce(String email) {
        emailBounceRegistry.registerBounce(email);
    }
}
