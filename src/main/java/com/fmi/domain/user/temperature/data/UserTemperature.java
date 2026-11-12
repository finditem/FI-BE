package com.fmi.domain.user.temperature.data;

import com.fmi.domain.user.user.data.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 주간 재계산 결과. 활동이 있는 사용자만 행이 있다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_temperature")
public class UserTemperature {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "temperature_tenths", nullable = false)
    private int temperatureTenths;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    public TemperatureSnapshot toSnapshot() {
        return new TemperatureSnapshot(new Temperature(temperatureTenths), calculatedAt);
    }
}
