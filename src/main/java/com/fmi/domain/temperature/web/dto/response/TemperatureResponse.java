package com.fmi.domain.temperature.web.dto.response;

import com.fmi.domain.temperature.data.TemperatureSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TemperatureResponse(
        @Schema(description = "찾길 온도. 소수 첫째 자리까지, 0.0 ~ 99.0", example = "36.5")
        BigDecimal temperature,

        @Schema(description = "온도를 계산한 시각. 아직 계산된 적 없으면 null")
        LocalDateTime calculatedAt) {

    public static TemperatureResponse from(TemperatureSnapshot snapshot) {
        return new TemperatureResponse(snapshot.temperature().celsius(), snapshot.calculatedAt());
    }
}
