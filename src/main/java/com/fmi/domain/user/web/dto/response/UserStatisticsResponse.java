package com.fmi.domain.user.web.dto.response;

import com.fmi.domain.user.repository.UserStatisticsResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserStatisticsResponse(
        @Schema(description = "종료일 기준 전체 수", example = "0") long totalUserCount,
        @Schema(description = "기간 중 가입 수", example = "0") long signupCount,
        @Schema(description = "기간 중 탈퇴 수", example = "0") long withdrawalCount) {
    public static UserStatisticsResponse from(UserStatisticsResult result) {
        return new UserStatisticsResponse(result.totalUserCount(), result.signupCount(), result.withdrawalCount());
    }
}
