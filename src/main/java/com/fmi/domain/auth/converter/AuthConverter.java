package com.fmi.domain.auth.converter;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.web.response.EmailVerifyResponse;
import com.fmi.domain.auth.web.response.LoginResponse;
import com.fmi.domain.auth.web.response.SignupResponse;
import com.fmi.domain.user.data.User;
import org.springframework.stereotype.Component;

@Component
public class AuthConverter {

    public static User toSocialUserEntity(
            Long providerId, String email, String nickname, String profileImageUrl, String encodedPassword) {
        String effectiveEmail = (email != null && !email.isBlank()) ? email : ("kakao_" + providerId + "@kakao.local");

        return User.builder()
                .email(effectiveEmail)
                .password(encodedPassword)
                .nickname(nickname != null ? nickname : ("kakao_" + providerId))
                .profile_img(profileImageUrl != null ? profileImageUrl : "")
                .role(Role.USER)
                .email_verified(true)
                .privacyPolicyAgreed(false)
                .termsOfServiceAgreed(false)
                .contentPolicyAgreed(false)
                .marketingConsent(false)
                .build();
    }

    public static SignupResponse toSignupResponse(Long userId) {
        return new SignupResponse(userId);
    }

    public static LoginResponse toLoginResponse(Long userId, boolean isTemporaryPassword) {
        return new LoginResponse(userId, isTemporaryPassword);
    }

    public static LoginResponse toLoginResponse(Long userId) {
        return new LoginResponse(userId, false);
    }

    public static EmailVerifyResponse toEmailVerifyResponse(boolean verified) {
        return new EmailVerifyResponse(verified);
    }
}
