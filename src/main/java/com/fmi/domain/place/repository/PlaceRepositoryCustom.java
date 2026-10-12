package com.fmi.domain.place.repository;

import com.fmi.domain.Enum.LanguageCode;
import com.fmi.domain.place.data.HomePlace;
import com.fmi.domain.place.data.enums.PlaceType;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public interface PlaceRepositoryCustom {
    default List<HomePlace> findHomePlaces(LocalDateTime now, DayOfWeek today, DayOfWeek yesterday, Long userId) {
        return findHomePlaces(now, today, yesterday, userId, LanguageCode.KO);
    }

    default List<HomePlace> findHomePlaces(
            PlaceType type, LocalDateTime now, DayOfWeek today, DayOfWeek yesterday, Long userId) {
        return findHomePlaces(type, now, today, yesterday, userId, LanguageCode.KO);
    }

    List<HomePlace> findHomePlaces(
            LocalDateTime now, DayOfWeek today, DayOfWeek yesterday, Long userId, LanguageCode language);

    List<HomePlace> findHomePlaces(
            PlaceType type,
            LocalDateTime now,
            DayOfWeek today,
            DayOfWeek yesterday,
            Long userId,
            LanguageCode language);

    List<Long> findMapCandidateIds(
            PlaceType type,
            double latitude,
            double longitude,
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude);
}
