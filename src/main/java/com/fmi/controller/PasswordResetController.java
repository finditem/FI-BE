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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth/reset")
@RequiredArgsConstructor
@Tag(name = "Auth-Reset", description = "비밀번호 재설정 API")
public class PasswordResetController {

    private final PasswordResetService resetService;

    @PostMapping("/request")
    @Operation(summary = "비밀번호 재설정 토큰 발급",
            description = "이메일을 입력하면 재설정 토큰을 발급합니다. 실제 서비스에서는 이메일로 재설정 링크를 전송합니다.")
    public ApiResponse<RequestResponse> request(@Valid @RequestBody RequestDto dto) {
        String token = resetService.requestReset(dto.getEmail());
        return ApiResponse.onSuccess(new RequestResponse(token));
    }

    @PostMapping("/confirm")
    @Operation(summary = "비밀번호 재설정 확정",
            description = "발급받은 토큰과 새 비밀번호를 제출하면 비밀번호를 변경합니다.")
    public ApiResponse<Void> confirm(@Valid @RequestBody ConfirmDto dto) {
        resetService.confirmReset(dto.getToken(), dto.getNewPassword());
        return ApiResponse.onSuccess(null);
    }

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


