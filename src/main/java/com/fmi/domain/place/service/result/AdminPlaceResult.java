package com.fmi.domain.place.service.result;

import com.fmi.domain.place.data.enums.PlaceType;
import java.time.LocalDateTime;

public record AdminPlaceResult(
        Long placeId,
        String name,
        String nameEn,
        PlaceType type,
        String address,
        String thumbnailUrl,
        int stationDistanceMeters,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {}
