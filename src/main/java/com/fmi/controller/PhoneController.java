package com.fmi.controller;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.service.PhoneVerificationService;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/phone")
@RequiredArgsConstructor
@Tag(name = "Phone", description = "휴대폰 인증 코드 발송/검증 API")
public class PhoneController {

    private final PhoneVerificationService service;

    @PostMapping("/send-code")
    @Operation(summary = "휴대폰 인증 코드 발송", description = "휴대폰 번호로 6자리 인증번호를 발송합니다.")
    public ApiResponse<Void> send(@RequestBody SendRequest req) {
        service.sendCode(req.getPhone());
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/verify")
    @Operation(summary = "휴대폰 인증 코드 검증", description = "코드 검증 후 사용자의 휴대폰 인증을 true로 설정합니다.")
    public ApiResponse<VerifyResponse> verify(@RequestBody VerifyRequest req) {
        boolean ok = service.verifyAndMark(req.getPhone(), req.getCode());
        return ApiResponse.onSuccess(new VerifyResponse(ok));
    }

    @Data
    public static class SendRequest {
        @NotBlank
        private String phone;
    }

    @Data
    public static class VerifyRequest {
        @NotBlank
        private String phone;
        @NotBlank
        private String code;
    }

    @Data
    @AllArgsConstructor
    public static class VerifyResponse {
        private boolean verified;
    }
}


