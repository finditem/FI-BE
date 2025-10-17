package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.response.EmailVerifyResponse;
import com.fmi.domain.auth.service.EmailVerificationService;
import com.fmi.domain.auth.web.dto.EmailSendRequest;
import com.fmi.domain.auth.web.dto.EmailVerifyRequest;
import com.fmi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth/email")
@RequiredArgsConstructor
@Tag(name = "Auth-Email", description = "이메일 인증 코드 발송/검증 API")
public class EmailController {

    private final EmailVerificationService service;

    @PostMapping("/send-code")
    @Operation(summary = "인증 코드 발송", description = "이메일로 6자리 인증번호를 발송합니다.")
    public ApiResponse<Void> send(@RequestBody EmailSendRequest req) {
        service.sendCode(req.getEmail());
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/verify")
    @Operation(summary = "인증 코드 검증", description = "이메일과 인증번호가 일치하고 유효기간 내인지 확인합니다.")
    public ApiResponse<EmailVerifyResponse> verify(@RequestBody EmailVerifyRequest req) {
        boolean ok = service.verify(req.getEmail(), req.getCode());
        return ApiResponse.onSuccess(new EmailVerifyResponse(ok));
    }
}


