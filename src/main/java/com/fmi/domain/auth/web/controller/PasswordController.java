package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.service.PasswordService;
import com.fmi.domain.auth.web.dto.request.PasswordChangeRequest;
import com.fmi.domain.auth.web.dto.request.PasswordVerifyRequest;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Auth")
public class PasswordController {

    private final PasswordService passwordService;

    @PostMapping("/users/me/password/verify")
    public ApiResponse<Void> verifyPassword(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PasswordVerifyRequest request) {
        passwordService.verify(userDetails.getUsername(), request);
        return ApiResponse.onSuccess(null);
    }

    @PatchMapping("/users/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PasswordChangeRequest request) {
        passwordService.change(userDetails.getUsername(), request.getNewPassword(), request.getNewPasswordConfirm());
        return ApiResponse.onSuccess(null);
    }
}
