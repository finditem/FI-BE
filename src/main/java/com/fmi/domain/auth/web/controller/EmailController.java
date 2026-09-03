package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.service.EmailVerificationService;
import com.fmi.domain.auth.web.dto.EmailSendRequest;
import com.fmi.domain.auth.web.dto.EmailVerifyRequest;
import com.fmi.domain.auth.web.response.EmailVerifyResponse;
import com.fmi.domain.auth.web.swagger.EmailSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.service.EmailBounceHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/email")
@RequiredArgsConstructor
public class EmailController implements EmailSwagger {

    private final EmailVerificationService service;
    private final EmailBounceHandler emailBounceHandler;

    @PostMapping("/send-code")
    @Override
    public ApiResponse<Void> send(@Valid @RequestBody EmailSendRequest req) {
        service.sendCode(req.getEmail());
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/verify")
    @Override
    public ApiResponse<EmailVerifyResponse> verify(@Valid @RequestBody EmailVerifyRequest req) {
        service.verify(req.getEmail(), req.getCode());
        // 인증 성공 시에만 여기 도달
        return ApiResponse.onSuccess(AuthConverter.toEmailVerifyResponse(true));
    }

    @PostMapping("/bounce")
    @Override
    public ApiResponse<Void> registerBounce(@Valid @RequestBody EmailSendRequest req) {
        emailBounceHandler.registerBounce(req.getEmail());
        return ApiResponse.onSuccess(null);
    }
}
