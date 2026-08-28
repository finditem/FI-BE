package com.fmi.domain.user.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "중복 확인 응답")
public record CheckResponse(
        @Schema(description = "사용 가능 여부 (true: 사용 가능, false: 중복)", example = "true")
        boolean available) {}
