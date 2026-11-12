package com.fmi.domain.place.service;

import com.fmi.domain.place.data.Place;
import com.fmi.domain.place.data.PlaceDailySchedule;
import com.fmi.domain.place.data.PlaceOperationPeriod;
import com.fmi.domain.place.data.PlaceOperationState;
import com.fmi.domain.place.data.PlaceSummary;
import com.fmi.domain.place.data.PlaceTranslation;
import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.exception.PlaceErrorStatus;
import com.fmi.domain.place.repository.PlaceFavoriteRepository;
import com.fmi.domain.place.repository.PlaceRepository;
import com.fmi.domain.place.repository.PlaceTranslationRepository;
import com.fmi.domain.place.service.internal.PlaceOperationStatusCalculator;
import com.fmi.domain.user.user.data.enums.LanguageCode;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceQueryService {

    private final PlaceRepository placeRepository;

    private final PlaceTranslationRepository placeTranslationRepository;

    private final PlaceFavoriteRepository placeFavoriteRepository;

    private final UserRepository userRepository;

    private final PlaceOperationStatusCalculator placeOperationStatusCalculator;

    private final Clock clock;

    public PlaceSummary getPlaceSummary(Long placeId, String userEmail) {
        return getPlaceSummary(placeId, userEmail, LanguageCode.KO);
    }

    public PlaceSummary getPlaceSummary(Long placeId, String userEmail, LanguageCode language) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();
        DayOfWeek yesterdayDayOfWeek = today.minusDays(1).getDayOfWeek();
        List<DayOfWeek> relevantDayOfWeeks = List.of(todayDayOfWeek, yesterdayDayOfWeek);
        List<Long> placeIds = List.of(placeId);
        Place place = placeRepository.findAllWithSchedulesByIdInAndDayOfWeekIn(placeIds, relevantDayOfWeeks).stream()
                .findFirst()
                .orElseThrow(() -> new GeneralException(PlaceErrorStatus.NOT_FOUND));
        if (place.isDeleted()) {
            throw new GeneralException(PlaceErrorStatus.NOT_FOUND);
        }
        if (place.getType() == PlaceType.POPUP) {
            PlaceOperationPeriod operationPeriod = place.getOperationPeriod();
            LocalDateTime closingAt = operationPeriod.getClosingAt();
            if (!now.isBefore(closingAt)) {
                throw new GeneralException(PlaceErrorStatus.NOT_FOUND);
            }
        }

        boolean favorite = false;
        if (userEmail != null) {
            favorite = userRepository
                    .findByEmail(userEmail)
                    .flatMap(user -> placeFavoriteRepository.findByUser_IdAndPlace_Id(user.getId(), placeId))
                    .filter(placeFavorite -> placeFavorite.isActive() && placeFavorite.isFavorite())
                    .isPresent();
        }
        PlaceOperationPeriod operationPeriod = place.getOperationPeriod();
        List<PlaceDailySchedule> dailySchedules = place.dailySchedules(relevantDayOfWeeks);
        PlaceType placeType = place.getType();
        PlaceOperationState operationState =
                placeOperationStatusCalculator.calculate(placeType, operationPeriod, dailySchedules, now);
        PlaceTranslation translation = null;
        if (language == LanguageCode.EN) {
            translation = placeTranslationRepository
                    .findByPlace_IdAndLanguageCode(placeId, language)
                    .orElse(null);
        }
        return PlaceSummary.from(place, operationState, favorite).translated(translation);
    }
}
