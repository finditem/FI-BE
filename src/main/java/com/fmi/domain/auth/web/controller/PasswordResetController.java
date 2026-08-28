package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.service.PasswordService;
import com.fmi.domain.auth.web.dto.PasswordResetRequest;
import com.fmi.domain.auth.web.swagger.PasswordResetSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/reset")
@RequiredArgsConstructor
public class PasswordResetController implements PasswordResetSwagger {

    private final PasswordService passwordService;

    @PostMapping("/request")
    @Override
    public ApiResponse<Void> request(@Valid @RequestBody PasswordResetRequest dto) {
        passwordService.issueTemporaryPassword(dto.getEmail());
        return ApiResponse.onSuccess(null);
    }
}
