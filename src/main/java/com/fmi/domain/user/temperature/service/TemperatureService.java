package com.fmi.domain.user.temperature.service;

import com.fmi.domain.user.temperature.data.Temperature;
import com.fmi.domain.user.temperature.data.TemperatureActivity;
import com.fmi.domain.user.temperature.data.enums.TemperatureActivityType;
import com.fmi.domain.user.temperature.repository.TemperatureActivityRepository;
import com.fmi.domain.user.temperature.repository.UserTemperatureRepository;
import com.fmi.domain.user.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TemperatureService {

    private static final int CALCULATION_PERIOD_YEARS = 1;

    private final TemperatureActivityRepository temperatureActivityRepository;
    private final UserTemperatureRepository userTemperatureRepository;
    private final Clock clock;
    private final UserRepository userRepository;

    // 활동을 만든 Use Case의 transaction에 참여해 원장이 빠지지 않게 한다. 온도는 다음 정기 계산에 반영된다.
    @Transactional
    public void record(TemperatureActivityType type, Long userId, Long sourceId, LocalDateTime occurredAt) {
        // 탈퇴로 대상 사용자가 없으면 반영할 곳이 없다.
        if (userId == null) {
            return;
        }
        temperatureActivityRepository.save(TemperatureActivity.builder()
                .user(userRepository.getReferenceById(userId))
                .activityType(type)
                .sourceId(sourceId)
                .occurredAt(occurredAt)
                .build());
    }

    @Transactional
    public int recalculate() {
        LocalDateTime now = LocalDateTime.now(clock);
        return userTemperatureRepository.recalculateAll(
                now.minusYears(CALCULATION_PERIOD_YEARS),
                now,
                Temperature.INITIAL_TENTHS,
                Temperature.MIN_TENTHS,
                Temperature.MAX_TENTHS);
    }
}
