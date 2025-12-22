package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.service.PasswordResetService;
import com.fmi.domain.auth.web.dto.PasswordResetRequest;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시 비밀번호 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "COMMON400: 잘못된 요청입니다 (이메일 형식 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "AUTH400-SOCIAL_ACCOUNT: 소셜 로그인 계정입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH400-SOCIAL_ACCOUNT\", \"message\": \"소셜 로그인 계정입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "COMMON400: 잘못된 요청입니다 (존재하지 않는 이메일)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "AUTH500-EMAIL_SEND_FAILED: 이메일 발송에 실패했습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH500-EMAIL_SEND_FAILED\", \"message\": \"이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500: 서버 에러",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON500\", \"message\": \"서버 에러, 관리자에게 문의 바랍니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<Void> request(@Valid @RequestBody PasswordResetRequest dto) {
        resetService.issueTemporaryPassword(dto.getEmail());
        return ApiResponse.onSuccess(null);
    }
}


