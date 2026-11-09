package com.fmi.domain.auth.web.swagger;

import com.fmi.domain.auth.web.dto.request.PasswordChangeRequest;
import com.fmi.domain.auth.web.dto.request.PasswordVerifyRequest;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.userdetails.UserDetails;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Auth")
public interface PasswordSwagger {

    @Operation(summary = "현재 비밀번호 확인", description = "로그인한 사용자의 현재 비밀번호가 일치하는지 확인합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "현재 비밀번호 확인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "USER400-PASSWORD_INCORRECT: 현재 비밀번호가 일치하지 않습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "COMMON401: 로그인이 필요합니다.")
    })
    ApiResponse<Void> verifyPassword(@Parameter(hidden = true) UserDetails userDetails, PasswordVerifyRequest request);

    @Operation(summary = "비밀번호 변경", description = "로그인한 사용자의 비밀번호를 변경합니다. 새 비밀번호와 확인 값이 일치해야 합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description =
                        "USER400-PASSWORD_MISMATCH: 새 비밀번호와 확인 값이 다릅니다. AUTH400-WEAK_PASSWORD: 새 비밀번호가 규칙을 충족하지 않습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "COMMON401: 로그인이 필요합니다.")
    })
    ApiResponse<Void> changePassword(@Parameter(hidden = true) UserDetails userDetails, PasswordChangeRequest request);
}
