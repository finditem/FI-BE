package com.fmi.domain.auth.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.response.*;
import com.fmi.domain.auth.web.dto.*;
import com.fmi.domain.Enum.Role;
import org.springframework.stereotype.Component;

@Component
public class AuthConverter {

    /**
     * SignupRequest → User Entity 변환 (일반 회원가입용)
     * Role은 항상 USER로 고정됩니다.
     * emailVerified는 백엔드에서 검증한 값만 사용합니다 (보안).
     */
    public static User toUserEntity(SignupRequest request, String encodedPassword, boolean emailVerified) {
        return User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .role(Role.USER) // 일반 회원가입은 항상 USER로 고정
                .termsOfServiceAgreed(Boolean.TRUE.equals(request.getTermsOfServiceAgreed()))
                .privacyPolicyAgreed(Boolean.TRUE.equals(request.getPrivacyPolicyAgreed()))
                .marketingConsent(Boolean.TRUE.equals(request.getMarketingConsent()))
                .email_verified(emailVerified) // 백엔드에서 검증한 값만 사용
                .build();
    }

    /**
     * AdminSignupRequest → User Entity 변환 (관리자 회원가입용)
     * Role은 항상 ADMIN으로 고정됩니다.
     * 동의 항목은 기본값으로 설정됩니다.
     */
    public static User toAdminUserEntity(com.fmi.domain.admin.web.dto.AdminSignupRequest request, String encodedPassword) {
        return User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .role(Role.ADMIN) // 관리자 회원가입은 항상 ADMIN으로 고정
                .termsOfServiceAgreed(false) // 관리자는 동의 항목 불필요
                .privacyPolicyAgreed(false)
                .marketingConsent(false)
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
                .profile_img(profileImageUrl != null ? profileImageUrl : "")
                .role(Role.USER)
                .email_verified(true)
                .termsOfServiceAgreed(false)
                .privacyPolicyAgreed(false)
                .marketingConsent(false)
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

