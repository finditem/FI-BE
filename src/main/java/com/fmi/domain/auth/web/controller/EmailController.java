package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.converter.AuthConverter;
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
@Tag(name = "Auth")
public class EmailController {

    private final EmailVerificationService service;

    @PostMapping("/send-code")
    @Operation(summary = "email 인증 코드 발송", description = "이메일 중복 검사를 수행하고, 중복이 아니면 이메일로 6자리 인증번호를 발송합니다. 중복이면 409 에러를 반환합니다.")
    public ApiResponse<Void> send(@RequestBody EmailSendRequest req) {
        service.sendCode(req.getEmail());
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/verify")
    @Operation(summary = "email 인증 코드 검증", description = "이메일과 인증번호가 일치하고 유효기간 내인지 확인합니다.")
    public ApiResponse<EmailVerifyResponse> verify(@RequestBody EmailVerifyRequest req) {
        boolean ok = service.verify(req.getEmail(), req.getCode());
        return ApiResponse.onSuccess(AuthConverter.toEmailVerifyResponse(ok));
    }
}


