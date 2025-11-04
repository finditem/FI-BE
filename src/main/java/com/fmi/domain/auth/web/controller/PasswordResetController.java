package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.service.PasswordResetService;
import com.fmi.domain.auth.web.dto.PasswordResetRequest;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth/reset")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class PasswordResetController {

    private final PasswordResetService resetService;

    @PostMapping("/request")
    @Operation(summary = "임시 비밀번호 발급",
            description = """
                이메일을 입력하면 임시 비밀번호를 발급하여 이메일로 전송합니다.
                
                **임시 비밀번호 정책:**
                - 유효기한: 1시간
                - 1시간 경과 후 자동으로 만료되고 원래 비밀번호로 복원됩니다
                - 임시 비밀번호로 로그인 시 isTemporaryPassword 플래그가 true로 반환되며, 
                  프론트에서 비밀번호 변경 페이지로 리다이렉트해야 합니다
                - 로그인 후 비밀번호를 반드시 변경하세요
                """)
    public ApiResponse<Void> request(@Valid @RequestBody PasswordResetRequest dto) {
        resetService.issueTemporaryPassword(dto.getEmail());
        return ApiResponse.onSuccess(null);
    }
}


