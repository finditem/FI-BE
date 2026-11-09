package com.fmi.domain.place.service;

import com.fmi.domain.place.data.HomePlace;
import com.fmi.domain.place.data.PlaceOperationPeriod;
import com.fmi.domain.place.data.PlaceOperationState;
import com.fmi.domain.place.data.PlaceSummary;
import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.repository.PlaceHomeRepository;
import com.fmi.domain.place.service.internal.PlaceOperationStatusCalculator;
import com.fmi.domain.user.user.data.enums.LanguageCode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceHomeQueryService {
    private final PlaceHomeRepository placeHomeRepository;

    private final PlaceOperationStatusCalculator placeOperationStatusCalculator;

    private final Clock clock;

    public List<PlaceSummary> getHomePlaces(PlaceType type, Long userId) {
        return getHomePlaces(type, userId, LanguageCode.KO);
    }

    public List<PlaceSummary> getHomePlaces(PlaceType type, Long userId, LanguageCode language) {
        // 자정을 넘긴 전날 영업시간을 판단하기 위해 오늘과 전날 요일을 조회
        Instant currentInstant = clock.instant();
        LocalDateTime now = LocalDateTime.ofInstant(currentInstant, clock.getZone());
        LocalDateTime nowInUtc = LocalDateTime.ofInstant(currentInstant, ZoneOffset.UTC);
        LocalDate today = now.toLocalDate();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();
        DayOfWeek yesterdayDayOfWeek = today.minusDays(1).getDayOfWeek();

        // 타입별로 조회
        List<HomePlace> places;
        if (type == null) {
            places = placeHomeRepository.findHomePlaces(nowInUtc, todayDayOfWeek, yesterdayDayOfWeek, userId, language);
        } else {
            places = placeHomeRepository.findHomePlaces(
                    type, nowInUtc, todayDayOfWeek, yesterdayDayOfWeek, userId, language);
        }

        // 운영 상태 계산
        List<PlaceSummary> summaries = new ArrayList<>(places.size());
        for (HomePlace place : places) {
            PlaceOperationPeriod operationPeriod = null;
            if (place.type() == PlaceType.POPUP) {
                operationPeriod = PlaceOperationPeriod.builder()
                        .startDate(place.operationStartDate())
                        .endDate(place.operationEndDate())
                        .build();
            }

            PlaceOperationState operationState = placeOperationStatusCalculator.calculate(
                    place.type(), operationPeriod, place.dailySchedules(), now);
            // 운영 상태와 함께 응답 VO 변환
            summaries.add(PlaceSummary.from(place, operationState));
        }
        return summaries;
    }
}
