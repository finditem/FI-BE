package com.fmi.domain.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "이메일 인증 응답")
public class EmailVerifyResponse {
    @Schema(description = "이메일 인증 여부", example = "true")
    private boolean verified;
}

