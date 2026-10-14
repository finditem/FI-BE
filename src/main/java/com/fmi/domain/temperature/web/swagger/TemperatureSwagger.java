package com.fmi.domain.temperature.web.swagger;

import com.fmi.domain.temperature.web.dto.response.TemperatureResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Temperature", description = "찾길 온도 API")
public interface TemperatureSwagger {

    @Operation(
            summary = "내 찾길 온도 조회",
            description = "마이페이지 상단의 찾길 온도를 조회합니다. 매주 월요일 04:00에 갱신된 값이며, "
                    + "새 활동은 다음 갱신 때 반영됩니다. 계산된 적 없는 사용자는 36.5를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "찾길 온도 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다.")
    })
    ApiResponse<TemperatureResponse> getMyTemperature(@AuthenticationPrincipal UserDetails userDetails);
}
