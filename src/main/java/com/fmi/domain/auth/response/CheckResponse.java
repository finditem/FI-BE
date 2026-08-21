package com.fmi.domain.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "중복 확인 응답")
public class CheckResponse {
    @Schema(description = "사용 가능 여부 (true: 사용 가능, false: 중복)", example = "true")
    private boolean available;
}
