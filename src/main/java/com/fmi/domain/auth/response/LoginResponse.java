package com.fmi.domain.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "로그인 응답")
public class LoginResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    @Schema(description = "임시 비밀번호로 로그인했는지 여부 (비밀번호 변경 페이지 리다이렉트용)", example = "false")
    private boolean isTemporaryPassword;
    @Schema(description = "신규 가입 여부 (카카오 로그인 시 약관 동의 화면 분기용, 일반 로그인은 항상 false)", example = "false")
    private boolean isNewUser;

    public LoginResponse(Long userId, boolean isTemporaryPassword) {
        this.userId = userId;
        this.isTemporaryPassword = isTemporaryPassword;
        this.isNewUser = false;
    }
}

