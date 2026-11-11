package com.fmi.global.web.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@Tag(name = "Health", description = "헬스체크용 API")
public interface HealthSwagger {

    @SecurityRequirements
    @Operation(summary = "서버 상태 확인", description = "애플리케이션이 요청을 처리할 수 있으면 status에 UP을 반환합니다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서버 상태 확인 성공"))
    Map<String, Object> health();
}
