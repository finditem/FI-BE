package com.fmi.controller;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth/reset")
@RequiredArgsConstructor
@Tag(name = "Auth-Reset", description = "비밀번호 재설정 API")
public class PasswordResetController {

    private final PasswordResetService resetService;

    @PostMapping("/request")
    @Operation(summary = "임시 비밀번호 발급",
            description = "이메일을 입력하면 임시 비밀번호를 발급하여 이메일로 전송합니다.")
    public ApiResponse<Void> request(@Valid @RequestBody RequestDto dto) {
        resetService.issueTemporaryPassword(dto.getEmail());
        return ApiResponse.onSuccess(null);
    }

    // 토큰 기반 확정 API는 임시 비밀번호 방식으로 대체되어 비활성화

    @Data
    public static class RequestDto {
        @Schema(description = "가입 이메일", example = "user@example.com")
        @Email
        @NotBlank
        private String email;
    }

    @Data
    public static class ConfirmDto {
        @Schema(description = "재설정 토큰", example = "6398d390-0c9f-442f-84dd-ea73a303ea62")
        @NotBlank
        private String token;
        @Schema(description = "새 비밀번호(규칙 충족: 8자 이상, 대소문자/숫자/특수문자)", example = "Abcd1234!")
        @NotBlank
        private String newPassword;
    }

    @Data
    @AllArgsConstructor
    public static class RequestResponse {
        private String token;
    }
}


