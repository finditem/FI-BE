package com.fmi.domain.post.web.swagger;

import com.fmi.domain.post.web.dto.response.PostStatisticsResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;

public interface PostStatisticsSwagger {
    @Operation(
            tags = "Post",
            summary = "관리자 게시글 기간 통계 조회",
            description = "한국 시간 기준 시작일과 종료일을 포함합니다. 총수는 종료일 기준입니다. 도입 당일 발생량은 초기화 시각 이후만 포함합니다.",
            security = @SecurityRequirement(name = "BearerToken"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "기간 통계 조회"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "날짜 누락, 형식 오류, 기간 역전, 도입일 이전 또는 미래 날짜"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "통계 초기화 전")
    })
    ApiResponse<PostStatisticsResponse> get(LocalDate startDate, LocalDate endDate);
}
