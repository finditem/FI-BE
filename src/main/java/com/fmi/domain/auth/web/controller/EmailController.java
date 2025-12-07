package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.response.EmailVerifyResponse;
import com.fmi.domain.auth.service.EmailVerificationService;
import com.fmi.domain.auth.web.dto.EmailSendRequest;
import com.fmi.domain.auth.web.dto.EmailVerifyRequest;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.service.EmailBounceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth/email")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class EmailController {

    private final EmailVerificationService service;
    private final EmailBounceHandler emailBounceHandler;

    @PostMapping("/send-code")
    @Operation(summary = "email 인증 코드 발송", description = "이메일 중복 검사를 수행하고, 중복이 아니면 이메일로 6자리 인증번호를 발송합니다. 중복이면 409 에러를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "AUTH409-EMAIL_DUPLICATED: 이미 사용 중인 이메일입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH409-EMAIL_DUPLICATED\", \"message\": \"이미 사용 중인 이메일입니다.\"}"
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
                    responseCode = "400",
                    description = "COMMON400: 잘못된 요청입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<Void> send(@RequestBody EmailSendRequest req) {
        service.sendCode(req.getEmail());
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/verify")
    @Operation(summary = "email 인증 코드 검증", description = "이메일과 인증번호가 일치하고 유효기간 내인지 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 코드 검증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "AUTH400-EMAIL_VERIFY_FAILED: 인증 코드가 만료되었거나 일치하지 않습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH400-EMAIL_VERIFY_FAILED\", \"message\": \"인증 코드가 만료되었거나 일치하지 않습니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "COMMON400: 잘못된 요청입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<EmailVerifyResponse> verify(@RequestBody EmailVerifyRequest req) {
        service.verify(req.getEmail(), req.getCode());
        // 인증 성공 시에만 여기 도달
        return ApiResponse.onSuccess(AuthConverter.toEmailVerifyResponse(true));
    }

    @PostMapping("/bounce")
    @Operation(summary = "bounce back 수동 등록", description = "Gmail에서 bounce back을 받은 이메일 주소를 수동으로 등록합니다. 등록된 이메일 주소로는 인증 코드가 발송되지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bounce back 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "COMMON400: 잘못된 요청입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}"
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
    public ApiResponse<Void> registerBounce(@RequestBody EmailSendRequest req) {
        emailBounceHandler.registerBounce(req.getEmail());
        return ApiResponse.onSuccess(null);
    }
}


