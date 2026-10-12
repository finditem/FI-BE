package com.fmi.domain.place.web.dto.request;

import com.fmi.domain.place.data.PlaceDailySchedule;
import com.fmi.domain.place.data.PlaceTimeRange;
import com.fmi.domain.place.data.PlaceUpsertCommand;
import com.fmi.domain.place.data.enums.PlaceBusinessHourType;
import com.fmi.domain.place.data.enums.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PlaceUpsertRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 255) String address,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotBlank @Size(max = 100) String station,
        @NotNull @PositiveOrZero Integer stationDistanceMeters,
        @NotNull PlaceType type,
        @NotNull @Size(min = 7, max = 7) List<@Valid WeeklySchedule> weeklySchedules,
        @Valid OperationPeriod operationPeriod,

        @Schema(description = "영문 이름, 최대 100자. 앞뒤 공백 제거, 빈 값은 미등록. 수정 시 생략 또는 null은 기존 값을 제거합니다.", maxLength = 100)
        String nameEn,

        @Schema(description = "영문 주소, 최대 510자. 앞뒤 공백 제거, 빈 값은 미등록. 수정 시 생략 또는 null은 기존 값을 제거합니다.", maxLength = 510)
        String addressEn,

        @Schema(description = "영문 역 이름, 최대 200자. 앞뒤 공백 제거, 빈 값은 미등록. 수정 시 생략 또는 null은 기존 값을 제거합니다.", maxLength = 200)
        String stationEn) {

    public PlaceUpsertCommand toCommand() {
        return new PlaceUpsertCommand(
                name,
                address,
                latitude,
                longitude,
                station,
                stationDistanceMeters,
                type,
                operationPeriod == null ? null : operationPeriod.startDate(),
                operationPeriod == null ? null : operationPeriod.endDate(),
                weeklySchedules.stream().map(WeeklySchedule::toDailySchedule).toList(),
                nameEn,
                addressEn,
                stationEn);
    }

    public record WeeklySchedule(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull Boolean isClosed,
            @NotNull @Size(min = 1) List<@Valid TimeRange> timeRanges) {
        PlaceDailySchedule toDailySchedule() {
            return new PlaceDailySchedule(
                    dayOfWeek,
                    isClosed,
                    timeRanges.stream().map(TimeRange::toPlaceTimeRange).toList());
        }
    }

    public record TimeRange(
            @NotNull PlaceBusinessHourType type,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime) {
        PlaceTimeRange toPlaceTimeRange() {
            return new PlaceTimeRange(type, startTime, endTime);
        }
    }

    public record OperationPeriod(
            @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
}
