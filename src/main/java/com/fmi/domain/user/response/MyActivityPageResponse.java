package com.fmi.domain.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "내 활동 내역 통합 응답")
public record MyActivityPageResponse(
        @Schema(description = "날짜별 활동 내역 목록") List<DailyActivityResponse> activities,
        @Schema(description = "다음 페이지 커서 (ISO datetime)") String nextCursor,
        @Schema(description = "다음 페이지 존재 여부") boolean hasNext) {}
