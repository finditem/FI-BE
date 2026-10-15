package com.fmi.domain.temperature.data;

import java.time.LocalDateTime;

public record TemperatureSnapshot(Temperature temperature, LocalDateTime calculatedAt) {

    // 아직 계산된 적 없는 사용자
    public static TemperatureSnapshot initial() {
        return new TemperatureSnapshot(Temperature.initial(), null);
    }
}
