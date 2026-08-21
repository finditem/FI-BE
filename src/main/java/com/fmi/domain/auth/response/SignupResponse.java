package com.fmi.domain.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "회원가입 응답")
public class SignupResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
}
