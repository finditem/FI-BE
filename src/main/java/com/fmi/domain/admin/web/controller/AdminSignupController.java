package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.web.dto.request.AdminSignupRequest;
import com.fmi.domain.admin.web.swagger.AdminSignupSwagger;
import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.service.AuthService;
import com.fmi.domain.auth.web.dto.response.SignupResponse;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSignupController implements AdminSignupSwagger {

    private final AuthService authService;

    @PostMapping("/users/signup")
    @Override
    public ApiResponse<SignupResponse> adminSignup(@Valid @RequestBody AdminSignupRequest request) {
        Long id = authService.adminSignup(request);
        return ApiResponse.onSuccess(AuthConverter.toSignupResponse(id));
    }
}
