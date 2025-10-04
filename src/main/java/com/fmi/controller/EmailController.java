package com.fmi.controller;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.service.EmailVerificationService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "이메일 인증 코드 발송/검증 API")
public class EmailController {

    private final EmailVerificationService service;

    @PostMapping("/send-code")
    @Operation(summary = "인증 코드 발송", description = "이메일로 6자리 인증번호를 발송합니다.")
    public ApiResponse<Void> send(@RequestBody SendRequest req) {
        service.sendCode(req.getEmail());
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/verify")
    @Operation(summary = "인증 코드 검증", description = "이메일과 인증번호가 일치하고 유효기간 내인지 확인합니다.")
    public ApiResponse<VerifyResponse> verify(@RequestBody VerifyRequest req) {
        boolean ok = service.verify(req.getEmail(), req.getCode());
        return ApiResponse.onSuccess(new VerifyResponse(ok));
    }

    @Data
    public static class SendRequest {
        @Email @NotBlank
        private String email;
    }

    @Data
    public static class VerifyRequest {
        @Email @NotBlank
        private String email;
        @NotBlank
        private String code;
    }

    @Data
    @AllArgsConstructor
    public static class VerifyResponse {
        private boolean verified;
    }
}


