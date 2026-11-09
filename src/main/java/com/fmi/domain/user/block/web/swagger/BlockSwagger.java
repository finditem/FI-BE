package com.fmi.domain.user.block.web.swagger;

import com.fmi.domain.user.block.web.dto.response.BlockedUserResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Report", description = "신고/차단 API")
public interface BlockSwagger {

    @Operation(summary = "유저 차단", description = "로그인 사용자가 대상 유저를 차단합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 차단 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "USER400-BLOCK_SELF: 자기 자신은 차단할 수 없습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "USER409-ALREADY_BLOCKED: 이미 차단한 사용자입니다")
    })
    ApiResponse<String> block(@Parameter(hidden = true) UserDetails userDetails, Long userId);

    @Operation(summary = "유저 차단 해제", description = "로그인 사용자가 대상 유저의 차단을 해제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 차단 해제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = """
                        - USER404-NOT_FOUND: 존재하지 않는 회원입니다
                        - USER404-NOT_BLOCKED: 차단되지 않은 사용자입니다
                        """)
    })
    ApiResponse<String> unblock(@Parameter(hidden = true) UserDetails userDetails, Long userId);

    @Operation(summary = "내가 차단한 유저 목록", description = "차단한 유저의 ID, 닉네임, 프로필 사진을 커서 기반으로 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "차단 유저 목록 조회 성공")
    })
    ApiResponse<CursorPageResponse<BlockedUserResponse>> list(
            @Parameter(hidden = true) UserDetails userDetails, Long cursor, int size);
}
