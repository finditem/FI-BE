package com.fmi.controller;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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

    @Data
    public static class RequestDto {
        @Schema(description = "가입 이메일", example = "user@example.com")
        @Email
        private String email;
    }
}


