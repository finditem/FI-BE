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


    public static LoginResponse toLoginResponse(Long userId, boolean isTemporaryPassword) {
        // LoginResponse 생성자에 accessToken 파라미터를 제거했으므로
        // userId와 isTemporaryPassword만 전달합니다.
        return new LoginResponse(userId, isTemporaryPassword);
    }
    
    /**
     * LoginResponse 생성 메서드 (임시 비밀번호 플래그 없음 - 하위 호환성)
     * 
     * userId만 받아서 LoginResponse 객체를 생성합니다.
     * isTemporaryPassword는 기본값 false로 설정됩니다.
     * 
     * 변경 사항:
     * - accessToken 파라미터를 제거했습니다.
     * 
     * @param userId 사용자 ID
     * @return LoginResponse 객체 (isTemporaryPassword = false)
     */
    public static LoginResponse toLoginResponse(Long userId) {
        // isTemporaryPassword를 false로 설정하여 LoginResponse 생성
        return new LoginResponse(userId, false);
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

