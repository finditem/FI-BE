package com.fmi.domain.auth.web.swagger;

import com.fmi.domain.auth.web.dto.EmailSendRequest;
import com.fmi.domain.auth.web.dto.EmailVerifyRequest;
import com.fmi.domain.auth.web.response.EmailVerifyResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth")
public interface EmailSwagger {

    @Operation(
            summary = "email 인증 코드 발송",
            description = "이메일 중복 검사를 수행하고, 중복이 아니면 이메일로 6자리 인증번호를 발송합니다. 중복이면 409 에러를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "AUTH409-EMAIL_DUPLICATED: 이미 사용 중인 이메일입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"AUTH409-EMAIL_DUPLICATED\", \"message\": \"이미 사용 중인 이메일입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "AUTH500-EMAIL_SEND_FAILED: 이메일 발송에 실패했습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"AUTH500-EMAIL_SEND_FAILED\", \"message\": \"이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.\"}")))
    })
    ApiResponse<Void> send(@Valid @RequestBody EmailSendRequest req);

    @Operation(summary = "email 인증 코드 검증", description = "이메일과 인증번호가 일치하고 유효기간 내인지 확인합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 코드 검증 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "AUTH400-EMAIL_VERIFY_FAILED: 인증 코드가 만료되었거나 일치하지 않습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"AUTH400-EMAIL_VERIFY_FAILED\", \"message\": \"인증 코드가 만료되었거나 일치하지 않습니다.\"}")))
    })
    ApiResponse<EmailVerifyResponse> verify(@Valid @RequestBody EmailVerifyRequest req);

    @Operation(
            summary = "bounce back 수동 등록",
            description = "Gmail에서 bounce back을 받은 이메일 주소를 수동으로 등록합니다. 등록된 이메일 주소로는 인증 코드가 발송되지 않습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bounce back 등록 성공")
    })
    ApiResponse<Void> registerBounce(@Valid @RequestBody EmailSendRequest req);
}
