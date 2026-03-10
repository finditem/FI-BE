package com.fmi.domain.user.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "날짜별 활동 내역")
public record DailyActivityResponse(
        @Schema(description = "날짜 (yyyy-MM-dd)", example = "2024-01-01")
        String date,
        @Schema(description = "해당 날짜의 활동 목록")
        List<ActivityResponse> activities
) {
}
