package com.fmi.domain.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "로그인 응답")
public class LoginResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;  // 사용자 ID
    // accessToken 필드 제거됨 - 쿠키로 전송되므로 응답 body에 포함하지 않음
    @Schema(description = "임시 비밀번호로 로그인했는지 여부 (비밀번호 변경 페이지 리다이렉트용)", example = "false")
    private boolean isTemporaryPassword;  // 임시 비밀번호로 로그인했는지 여부 (비밀번호 변경 페이지 리다이렉트용)
}

