package com.fmi.domain.map.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "최근 습득된 분실물 조회 요청")
public record RecentFoundPostRequest(
        @Schema(description = "지도 중심 위도", example = "37.5665") @NotNull Double latitude,

        @Schema(description = "지도 중심 경도", example = "126.9780") @NotNull Double longitude,

        @Schema(description = "지도 줌 레벨 (1~11)", example = "5") @NotNull @Min(1) @Max(11) Integer level) {}
