package com.fmi.domain.place.repository;

import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.service.result.AdminPlaceResult;
import com.fmi.global.data.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface AdminPlaceRepositoryCustom {
    List<AdminPlaceResult> findAll(
            EntityStatus status, PlaceType type, String keyword, LocalDateTime cursorTime, Long cursorId, int limit);
}
