package com.fmi.domain.auth.web.response;

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

    @Schema(description = "필수 약관 동의 완료 여부 (false면 약관 동의 화면으로 이동 필요)", example = "true")
    private boolean termsAgreed;

    public LoginResponse(Long userId, boolean isTemporaryPassword) {
        this.userId = userId;
        this.isTemporaryPassword = isTemporaryPassword;
        this.termsAgreed = true;
    }
}
