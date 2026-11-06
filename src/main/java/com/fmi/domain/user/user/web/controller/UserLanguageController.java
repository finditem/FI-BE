package com.fmi.domain.user.user.web.controller;

import com.fmi.domain.user.user.service.UserLanguageService;
import com.fmi.domain.user.user.web.dto.request.PreferredLanguageUpdateRequest;
import com.fmi.domain.user.user.web.dto.response.PreferredLanguageResponse;
import com.fmi.domain.user.user.web.swagger.UserLanguageSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserLanguageController implements UserLanguageSwagger {
    private final UserLanguageService userLanguageService;

    @GetMapping("/me/preferred-language")
    @Override
    public ApiResponse<PreferredLanguageResponse> getPreferredLanguage(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        PreferredLanguageResponse response = userLanguageService.getPreferredLanguage(email);
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/me/preferred-language")
    @Override
    public ApiResponse<PreferredLanguageResponse> updatePreferredLanguage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PreferredLanguageUpdateRequest request) {
        String email = userDetails.getUsername();
        PreferredLanguageResponse response = userLanguageService.updatePreferredLanguage(email, request);
        return ApiResponse.onSuccess(response);
    }
}
