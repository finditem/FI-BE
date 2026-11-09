package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.security.cookie.AuthCookieFactory;
import com.fmi.domain.auth.service.WithdrawalService;
import com.fmi.domain.auth.web.dto.request.AccountDeleteRequest;
import com.fmi.domain.auth.web.swagger.WithdrawalSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class WithdrawalController implements WithdrawalSwagger {

    private final WithdrawalService withdrawalService;

    private final AuthCookieFactory authCookieFactory;

    @DeleteMapping("/users/me")
    @Override
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AccountDeleteRequest request,
            HttpServletRequest httpRequest) {
        withdrawalService.delete(userDetails.getUsername(), request);
        ResponseCookie accessCookie = authCookieFactory.expireAccessCookie(httpRequest);
        ResponseCookie refreshCookie = authCookieFactory.expireRefreshCookie(httpRequest);
        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(null));
    }
}
