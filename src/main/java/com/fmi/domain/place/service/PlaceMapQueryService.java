package com.fmi.domain.place.service;

import com.fmi.domain.place.data.Place;
import com.fmi.domain.place.data.PlaceDailySchedule;
import com.fmi.domain.place.data.PlaceMapSearchResult;
import com.fmi.domain.place.data.PlaceOperationPeriod;
import com.fmi.domain.place.data.PlaceOperationState;
import com.fmi.domain.place.data.PlaceSummary;
import com.fmi.domain.place.data.PlaceTranslation;
import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.repository.PlaceFavoriteRepository;
import com.fmi.domain.place.repository.PlaceMapRepository;
import com.fmi.domain.place.repository.PlaceRepository;
import com.fmi.domain.place.repository.PlaceTranslationRepository;
import com.fmi.domain.place.service.internal.PlaceOperationStatusCalculator;
import com.fmi.domain.post.post.data.enums.MapLevel;
import com.fmi.domain.user.user.data.enums.LanguageCode;
import com.fmi.domain.user.user.repository.UserRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceMapQueryService {
    private final PlaceMapRepository placeMapRepository;

    private final PlaceRepository placeRepository;

    private final PlaceTranslationRepository placeTranslationRepository;

    private final PlaceFavoriteRepository placeFavoriteRepository;

    private final UserRepository userRepository;

    private final PlaceOperationStatusCalculator placeOperationStatusCalculator;

    private final Clock clock;

    public PlaceMapSearchResult getMapPlaces(
            double latitude, double longitude, int level, PlaceType type, String userEmail) {
        return getMapPlaces(latitude, longitude, level, type, userEmail, LanguageCode.KO);
    }

    public PlaceMapSearchResult getMapPlaces(
            double latitude, double longitude, int level, PlaceType type, String userEmail, LanguageCode language) {
        MapLevel mapLevel = MapLevel.from(level);
        double latitudeDelta = mapLevel.getHalfHeightMeter() / 111_320.0;
        double latitudeRadian = Math.toRadians(latitude);
        double longitudeScale = Math.max(Math.abs(Math.cos(latitudeRadian)), 1e-8);
        double longitudeDelta = mapLevel.getHalfWidthMeter() / (111_320.0 * longitudeScale);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();
        DayOfWeek yesterdayDayOfWeek = today.minusDays(1).getDayOfWeek();
        List<DayOfWeek> relevantDayOfWeeks = List.of(todayDayOfWeek, yesterdayDayOfWeek);
        List<Long> candidateIds = placeMapRepository.findMapCandidateIds(
                type,
                latitude,
                longitude,
                latitude - latitudeDelta,
                latitude + latitudeDelta,
                longitude - longitudeDelta,
                longitude + longitudeDelta);
        if (candidateIds.isEmpty()) {
            return new PlaceMapSearchResult(List.of(), 0);
        }

        List<Place> candidates =
                placeRepository.findAllWithSchedulesByIdInAndDayOfWeekIn(candidateIds, relevantDayOfWeeks);
        candidates.sort(Comparator.comparingInt(place -> candidateIds.indexOf(place.getId())));
        List<Place> visiblePlaces = candidates.stream()
                .filter(place -> {
                    if (place.getType() != PlaceType.POPUP) {
                        return true;
                    }
                    PlaceOperationPeriod operationPeriod = place.getOperationPeriod();
                    LocalDateTime closingAt = operationPeriod.getClosingAt();
                    return now.isBefore(closingAt);
                })
                .toList();
        List<Place> places = visiblePlaces.stream().limit(10).toList();
        List<Long> placeIds = places.stream().map(Place::getId).toList();
        Set<Long> favoritePlaceIds = new HashSet<>();
        if (userEmail != null) {
            userRepository
                    .findByEmail(userEmail)
                    .ifPresent(user -> favoritePlaceIds.addAll(
                            placeFavoriteRepository.findFavoritePlaceIds(user.getId(), placeIds)));
        }

        Map<Long, PlaceTranslation> translations = Map.of();
        if (language == LanguageCode.EN && !placeIds.isEmpty()) {
            translations = placeTranslationRepository.findAllByPlace_IdInAndLanguageCode(placeIds, language).stream()
                    .collect(Collectors.toMap(
                            translation -> translation.getPlace().getId(), Function.identity()));
        }
        Map<Long, PlaceTranslation> translationsById = translations;
        List<PlaceSummary> summaries = places.stream()
                .map(place -> {
                    PlaceOperationPeriod operationPeriod = place.getOperationPeriod();
                    List<PlaceDailySchedule> dailySchedules = place.dailySchedules(relevantDayOfWeeks);
                    PlaceType placeType = place.getType();
                    PlaceOperationState operationState =
                            placeOperationStatusCalculator.calculate(placeType, operationPeriod, dailySchedules, now);
                    Long placeId = place.getId();
                    boolean favorite = favoritePlaceIds.contains(placeId);
                    return PlaceSummary.from(place, operationState, favorite).translated(translationsById.get(placeId));
                })
                .toList();
        return new PlaceMapSearchResult(summaries, visiblePlaces.size());
    }
}
