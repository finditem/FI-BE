package com.fmi.domain.place.web;

import com.fmi.domain.place.exception.PlaceErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public record AdminPlaceCursor(
        @Schema(description = "다음 페이지 기준 일시. 일반 목록은 등록 시각, 삭제 목록은 삭제 시각. 한국 표준시")
        LocalDateTime time,

        @Schema(description = "다음 페이지 기준 장소 ID") Long placeId) {
    public static AdminPlaceCursor parse(String cursorTime, String cursorPlaceId) {
        if (cursorTime == null && cursorPlaceId == null) {
            return null;
        }
        if (cursorTime == null || cursorPlaceId == null) {
            throw new GeneralException(PlaceErrorStatus.INVALID_REQUEST);
        }
        try {
            LocalDateTime time = LocalDateTime.parse(cursorTime);
            long placeId = Long.parseLong(cursorPlaceId);
            if (placeId <= 0 || time.getYear() < 1000 || time.getYear() > 9999 || time.getNano() % 1000 != 0) {
                throw new IllegalArgumentException("Invalid cursor boundary");
            }
            return new AdminPlaceCursor(time, placeId);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new GeneralException(PlaceErrorStatus.INVALID_REQUEST);
        }
    }
}
