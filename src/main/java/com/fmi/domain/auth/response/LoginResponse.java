package com.fmi.domain.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String accessToken;
    private boolean isTemporaryPassword;  // 임시 비밀번호로 로그인했는지 여부 (비밀번호 변경 페이지 리다이렉트용)
}

