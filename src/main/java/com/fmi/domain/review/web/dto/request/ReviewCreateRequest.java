package com.fmi.domain.review.web.dto.request;

import com.fmi.domain.review.data.ReviewEmotion;
import com.fmi.domain.review.data.ReviewHelpType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ReviewCreateRequest(
        @Schema(
                description = "감정 유형. MOVED(감동), GRATEFUL(감사), HEART_FLUTTERED(심쿵)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull ReviewEmotion emotion,

        @Schema(description = "도움 경험. 1개 이상 선택", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty Set<@NotNull ReviewHelpType> helpTypes,

        @Schema(description = "상세 후기. 선택 입력, 최대 300자. 공백만 입력하면 상세 후기 없이 저장합니다.")
        String content) {}
