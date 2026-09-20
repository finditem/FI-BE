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
            String email, String nickname, String profileImageUrl, String encodedPassword) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .profile_img(profileImageUrl)
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
