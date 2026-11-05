package com.fmi.domain.place.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.domain.place.exception.PlaceErrorStatus;
import com.fmi.domain.place.web.dto.request.AdminPlaceListRequest;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("AdminPlaceCursor")
class AdminPlaceCursorTest {
    @Nested
    @DisplayName("목록 커서의 시각과 장소 ID를 해석할 때")
    class DescribeParse {
        @Test
        @DisplayName("마이크로초 시각과 ID를 그대로 사용한다")
        void itPreservesTimestampAndId() {
            // given
            String time = "2026-11-04T10:00:00.123456";
            String placeId = "42";
            // when
            AdminPlaceCursor cursor = AdminPlaceCursor.parse(time, placeId);
            // then
            assertThat(cursor.time()).isEqualTo(LocalDateTime.of(2026, 11, 4, 10, 0, 0, 123456000));
            assertThat(cursor.placeId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("두 값을 모두 생략하면 첫 페이지로 처리한다")
        void itStartsAtFirstPageWithoutBothFields() {
            // given & when
            AdminPlaceCursor cursor = AdminPlaceCursor.parse(null, null);
            // then
            assertThat(cursor).isNull();
        }

        @ParameterizedTest
        @CsvSource(
                value = {
                    "NULL;42",
                    "2026-11-04T10:00;NULL",
                    "'';42",
                    "2026-11-04T10:00;''",
                    "not-time;42",
                    "2026-11-04T10:00;abc",
                    "2026-11-04T10:00;0",
                    "2026-11-04T10:00;-1",
                    "2026-11-04T10:00;9223372036854775808",
                    "2026-11-04T10:00:00.123456789;42",
                    "0999-01-01T00:00;42"
                },
                delimiter = ';',
                nullValues = "NULL")
        @DisplayName("한 값만 전달하거나 시각과 ID가 유효하지 않으면 PLACE400으로 거절한다")
        void itRejectsIncompleteOrInvalidBoundary(String time, String placeId) {
            // given & when & then
            assertThatThrownBy(() -> AdminPlaceCursor.parse(time, placeId))
                    .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                            .isEqualTo(PlaceErrorStatus.INVALID_REQUEST));
        }

        @Test
        @DisplayName("지원하지 않는 장소 유형을 PLACE400으로 거절한다")
        void itRejectsInvalidType() {
            // given
            AdminPlaceListRequest request = new AdminPlaceListRequest("cafe", null, null, null);
            // when & then
            assertThatThrownBy(request::placeType)
                    .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                            .isEqualTo(PlaceErrorStatus.INVALID_REQUEST));
        }
    }
}
