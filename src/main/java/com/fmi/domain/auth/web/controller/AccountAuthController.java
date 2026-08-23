package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.service.PasswordService;
import com.fmi.domain.auth.service.WithdrawalService;
import com.fmi.domain.auth.web.dto.AccountDeleteRequest;
import com.fmi.domain.auth.web.dto.PasswordChangeRequest;
import com.fmi.domain.auth.web.dto.PasswordVerifyRequest;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.CookieFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class AccountAuthController {

    private final PasswordService passwordService;
    private final WithdrawalService withdrawalService;
    private final CookieFactory cookieFactory;

    @Value("${jwt.cookie.name:refresh_token}")
    private String refreshCookieName;

    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessCookieName;

    @PostMapping("/password/verify")
    public ApiResponse<Void> verifyPassword(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PasswordVerifyRequest request) {
        passwordService.verify(userDetails.getUsername(), request);
        return ApiResponse.onSuccess(null);
    }

    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PasswordChangeRequest request) {
        passwordService.change(userDetails.getUsername(), request.getNewPassword(), request.getNewPasswordConfirm());
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AccountDeleteRequest request,
            HttpServletRequest httpRequest) {
        withdrawalService.delete(userDetails.getUsername(), request);
        ResponseCookie removeAccess = cookieFactory.expire(httpRequest, accessCookieName);
        ResponseCookie removeRefresh = cookieFactory.expire(httpRequest, refreshCookieName);
        return ResponseEntity.ok()
                .header("Set-Cookie", removeAccess.toString())
                .header("Set-Cookie", removeRefresh.toString())
                .body(ApiResponse.onSuccess(null));
    }
}
