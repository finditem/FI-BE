package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.response.PhoneVerifyResponse;
import com.fmi.domain.auth.service.PhoneVerificationService;
import com.fmi.domain.auth.web.dto.PhoneSendRequest;
import com.fmi.domain.auth.web.dto.PhoneVerifyRequest;
import com.fmi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth/phone")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class PhoneController {

    private final PhoneVerificationService service;

    @PostMapping("/send-code")
    @Operation(summary = "휴대폰 인증 코드 발송", description = "휴대폰 번호로 6자리 인증번호를 발송합니다.")
    public ApiResponse<Void> send(@RequestBody PhoneSendRequest req) {
        service.sendCode(req.getPhone());
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/verify")
    @Operation(summary = "휴대폰 인증 코드 검증", description = "코드 검증 후 사용자의 휴대폰 인증을 true로 설정합니다.")
    public ApiResponse<PhoneVerifyResponse> verify(@RequestBody PhoneVerifyRequest req) {
        boolean ok = service.verifyAndMark(req.getPhone(), req.getCode());
        return ApiResponse.onSuccess(AuthConverter.toPhoneVerifyResponse(ok));
    }
}


