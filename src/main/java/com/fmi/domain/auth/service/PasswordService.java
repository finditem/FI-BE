package com.fmi.domain.auth.service;

import com.fmi.domain.auth.web.dto.PasswordChangeRequest;
import com.fmi.domain.auth.web.dto.PasswordVerifyRequest;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.security.RefreshTokenStore;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;

    public void verify(String email, PasswordVerifyRequest request) {
        if (!matches(email, request.getCurrentPassword())) {
            throw new GeneralException(ErrorStatus._CURRENT_PASSWORD_INCORRECT);
        }
    }

    public void change(String email, PasswordChangeRequest request) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new GeneralException(ErrorStatus._PASSWORD_CONFIRMATION_MISMATCH);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setOriginalPassword(null);
        user.setTemporaryPassword(null);
        user.setTemporaryPasswordExpiresAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenStore.revokeAllForUser(email);
    }

    private boolean matches(String email, String password) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        boolean temporaryPasswordActive = user.getTemporaryPassword() != null
                && user.getTemporaryPasswordExpiresAt() != null
                && LocalDateTime.now().isBefore(user.getTemporaryPasswordExpiresAt());

        if (!temporaryPasswordActive) {
            return passwordEncoder.matches(password, user.getPassword());
        }

        return passwordEncoder.matches(password, user.getTemporaryPassword())
                || (user.getOriginalPassword() != null && passwordEncoder.matches(password, user.getOriginalPassword()))
                || passwordEncoder.matches(password, user.getPassword());
    }
}
