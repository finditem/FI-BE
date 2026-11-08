package com.fmi.domain.place.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("장소 운영기간")
class PlaceOperationPeriodTest {
    @Test
    @DisplayName("운영기간을 수정한다")
    void updatesPeriod() {
        PlaceOperationPeriod period = PlaceOperationPeriod.builder()
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 2))
                .build();
        period.revise(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2));
        assertThat(period.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    @DisplayName("팝업 종료 시각을 예약한다")
    void schedulesClosingAt() {
        PlaceOperationPeriod period = PlaceOperationPeriod.builder()
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 2))
                .build();
        LocalDateTime closingAt = LocalDateTime.of(2026, 1, 2, 22, 0);

        period.scheduleClosingAt(closingAt);

        assertThat(period.getClosingAt()).isEqualTo(closingAt);
    }
}
