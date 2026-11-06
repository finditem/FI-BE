package com.fmi.domain.place.repository;

import com.fmi.domain.place.data.HomePlace;
import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.user.user.data.enums.LanguageCode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public interface PlaceHomeRepository {

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
}
