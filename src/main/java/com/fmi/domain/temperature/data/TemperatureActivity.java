package com.fmi.domain.temperature.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 온도 산정 원장. 원본 게시글이나 후기가 삭제되어도 기록은 유지한다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "temperature_activity",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_temperature_activity_source",
                        columnNames = {"activity_type", "source_id"}))
public class TemperatureActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, updatable = false, length = 30)
    private TemperatureActivityType activityType;

    @Column(name = "source_id", nullable = false, updatable = false)
    private Long sourceId;

    // 가중치가 바뀌어도 이미 발생한 활동의 값은 유지한다.
    @Column(name = "delta_tenths", nullable = false, updatable = false)
    private int deltaTenths;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Builder
    private TemperatureActivity(
            Long userId, TemperatureActivityType activityType, Long sourceId, LocalDateTime occurredAt) {
        this.userId = userId;
        this.activityType = activityType;
        this.sourceId = sourceId;
        this.deltaTenths = activityType.getDeltaTenths();
        this.occurredAt = occurredAt;
    }
}
