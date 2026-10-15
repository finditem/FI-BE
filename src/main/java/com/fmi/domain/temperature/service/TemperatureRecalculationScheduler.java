package com.fmi.domain.temperature.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemperatureRecalculationScheduler {

    private final TemperatureService temperatureService;

    // 찾길 온도는 주 1회 정기 업데이트한다. 실패하면 직전 값이 유지되고 다음 주에 다시 계산한다.
    @Scheduled(cron = "0 0 4 * * MON", zone = "Asia/Seoul")
    public void recalculate() {
        int updatedCount = temperatureService.recalculate();
        log.info("찾길 온도 재계산 완료, updatedCount={}", updatedCount);
    }
}
