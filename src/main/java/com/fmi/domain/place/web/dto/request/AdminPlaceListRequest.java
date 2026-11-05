package com.fmi.domain.place.web.dto.request;

import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.exception.PlaceErrorStatus;
import com.fmi.domain.place.web.AdminPlaceCursor;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminPlaceListRequest(
        @Schema(
                description = "장소 유형. 생략하면 전체 유형",
                allowableValues = {"CAFE", "RESTAURANT", "POPUP"})
        String type,

        @Schema(description = "한국어 또는 영문 이름 부분 검색. 앞뒤 공백 제거, 영문 대소문자 구분 없음")
        String keyword,

        @Schema(
                description = "직전 nextCursor.time. cursorPlaceId와 함께 전달하며 첫 요청은 생략",
                type = "string",
                format = "date-time")
        String cursorTime,

        @Schema(description = "직전 nextCursor.placeId. cursorTime과 함께 전달하며 첫 요청은 생략", type = "integer", format = "int64")
        String cursorPlaceId) {
    public PlaceType placeType() {
        if (type == null) {
            return null;
        }
        try {
            return PlaceType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorStatus.INVALID_REQUEST);
        }
    }

    public AdminPlaceCursor boundary() {
        return AdminPlaceCursor.parse(cursorTime, cursorPlaceId);
    }
}
