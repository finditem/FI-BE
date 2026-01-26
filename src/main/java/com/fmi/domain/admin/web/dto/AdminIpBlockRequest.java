package com.fmi.domain.admin.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminIpBlockRequest {

    @Schema(description = "차단 사유", example = "스팸 문의 반복")
    private String reason;
}
