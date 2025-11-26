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
    public static User toUserEntity(SignupRequest request, String encodedPassword) {
        return User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .name(request.getNickname()) // name은 nickname과 동일하게 설정
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .termsOfServiceAgreed(Boolean.TRUE.equals(request.getTermsOfServiceAgreed()))
                .privacyPolicyAgreed(Boolean.TRUE.equals(request.getPrivacyPolicyAgreed()))
                .marketingConsent(Boolean.TRUE.equals(request.getMarketingConsent()))
                .email_verified(Boolean.TRUE.equals(request.getEmailVerified()))
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
    public static LoginResponse toLoginResponse(Long userId, String accessToken, boolean isTemporaryPassword) {
        return new LoginResponse(userId, accessToken, isTemporaryPassword);
    }
    
    /**
     * User + AccessToken → LoginResponse 변환 (임시 비밀번호 플래그 없음 - 하위 호환성)
     */
    public static LoginResponse toLoginResponse(Long userId, String accessToken) {
        return new LoginResponse(userId, accessToken, false);
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

}

