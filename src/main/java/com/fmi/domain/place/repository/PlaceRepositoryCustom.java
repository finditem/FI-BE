package com.fmi.domain.place.repository;

import com.fmi.domain.place.data.HomePlace;
import com.fmi.domain.place.data.enums.PlaceType;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public interface PlaceRepositoryCustom {
    List<HomePlace> findHomePlaces(LocalDateTime now, DayOfWeek today, DayOfWeek yesterday, Long userId);

    List<HomePlace> findHomePlaces(
            PlaceType type, LocalDateTime now, DayOfWeek today, DayOfWeek yesterday, Long userId);

    List<Long> findMapCandidateIds(
            PlaceType type,
            double latitude,
            double longitude,
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude);
}
