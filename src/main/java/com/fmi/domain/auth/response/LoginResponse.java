package com.fmi.domain.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long userId;  // 사용자 ID
    // accessToken 필드 제거됨 - 쿠키로 전송되므로 응답 body에 포함하지 않음
    private boolean isTemporaryPassword;  // 임시 비밀번호로 로그인했는지 여부 (비밀번호 변경 페이지 리다이렉트용)
}

