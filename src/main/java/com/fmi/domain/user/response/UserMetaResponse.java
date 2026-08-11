package com.fmi.domain.user.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 메타데이터 응답")
public record UserMetaResponse(
        @Schema(description = "유저 닉네임")
        String nickname
) {
}
