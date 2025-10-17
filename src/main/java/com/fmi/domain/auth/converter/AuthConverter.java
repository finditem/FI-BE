package com.fmi.domain.auth.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.response.*;
import com.fmi.domain.auth.web.dto.*;
import com.fmi.domain.Enum.Role;
import org.springframework.stereotype.Component;

@Component
public class AuthConverter {

    /**
     * SignupRequest → User Entity 변환
     */
    public static User toUserEntity(SignupRequest request, String encodedPassword, boolean preVerifiedPhone) {
        return User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .profile_img(request.getProfileImg() != null ? request.getProfileImg() : "")
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .termsOfServiceAgreed(Boolean.TRUE.equals(request.getTermsOfServiceAgreed()))
                .privacyPolicyAgreed(Boolean.TRUE.equals(request.getPrivacyPolicyAgreed()))
                .marketingConsent(Boolean.TRUE.equals(request.getMarketingConsent()))
                .trust_score(request.getTrustScore() != null ? request.getTrustScore() : 0L)
                .email_verified(Boolean.TRUE.equals(request.getEmailVerified()))
                .phone_verified(preVerifiedPhone || Boolean.TRUE.equals(request.getPhoneVerified()))
                .build();
    }

    /**
     * 소셜 로그인용 User Entity 생성
     */
    public static User toSocialUserEntity(Long providerId, String email, String nickname, String profileImageUrl, String encodedPassword) {
        String effectiveEmail = (email != null && !email.isBlank())
                ? email
                : ("kakao_" + providerId + "@kakao.local");

        return User.builder()
                .email(effectiveEmail)
                .password(encodedPassword)
                .nickname(nickname != null ? nickname : ("kakao_" + providerId))
                .name(nickname != null ? nickname : ("kakao_" + providerId))
                .profile_img(profileImageUrl != null ? profileImageUrl : "")
                .role(Role.USER)
                .email_verified(true)
                .phone_verified(false)
                .termsOfServiceAgreed(false)
                .privacyPolicyAgreed(false)
                .marketingConsent(false)
                .trust_score(0L)
                .build();
    }

    /**
     * User → SignupResponse 변환
     */
    public static SignupResponse toSignupResponse(Long userId) {
        return new SignupResponse(userId);
    }

    /**
     * User + AccessToken → LoginResponse 변환
     */
    public static LoginResponse toLoginResponse(Long userId, String accessToken) {
        return new LoginResponse(userId, accessToken);
    }

    /**
     * boolean → CheckResponse 변환
     */
    public static CheckResponse toCheckResponse(boolean available) {
        return new CheckResponse(available);
    }

    /**
     * boolean → EmailVerifyResponse 변환
     */
    public static EmailVerifyResponse toEmailVerifyResponse(boolean verified) {
        return new EmailVerifyResponse(verified);
    }

    /**
     * boolean → PhoneVerifyResponse 변환
     */
    public static PhoneVerifyResponse toPhoneVerifyResponse(boolean verified) {
        return new PhoneVerifyResponse(verified);
    }
}

