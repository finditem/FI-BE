package com.fmi.domain.auth.web.swagger;

import com.fmi.domain.auth.web.dto.request.AccountDeleteRequest;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Auth")
public interface WithdrawalSwagger {

    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자의 계정을 삭제하고 인증 쿠키를 만료시킵니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "COMMON400: 기타 탈퇴 사유를 선택했으나 사유를 입력하지 않았습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "COMMON401: 로그인이 필요합니다.")
    })
    ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Parameter(hidden = true) UserDetails userDetails,
            AccountDeleteRequest request,
            HttpServletRequest httpRequest);
}
