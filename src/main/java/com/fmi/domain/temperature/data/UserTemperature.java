package com.fmi.domain.temperature.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 주간 재계산 결과. 활동이 있는 사용자만 행이 있다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_temperature")
public class UserTemperature {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "temperature_tenths", nullable = false)
    private int temperatureTenths;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    public TemperatureSnapshot toSnapshot() {
        return new TemperatureSnapshot(new Temperature(temperatureTenths), calculatedAt);
    }
}
