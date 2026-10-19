package com.fmi.domain.place.service;

import com.fmi.domain.Enum.LanguageCode;
import com.fmi.domain.place.data.FavoritePlaceCandidate;
import com.fmi.domain.place.data.FavoritePlacePage;
import com.fmi.domain.place.data.Place;
import com.fmi.domain.place.data.PlaceDailySchedule;
import com.fmi.domain.place.data.PlaceOperationPeriod;
import com.fmi.domain.place.data.PlaceOperationState;
import com.fmi.domain.place.data.PlaceSummary;
import com.fmi.domain.place.data.PlaceTranslation;
import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.exception.PlaceErrorStatus;
import com.fmi.domain.place.repository.PlaceFavoriteRepository;
import com.fmi.domain.place.repository.PlaceFavoriteStateRepository;
import com.fmi.domain.place.repository.PlaceRepository;
import com.fmi.domain.place.repository.PlaceTranslationRepository;
import com.fmi.domain.place.service.internal.PlaceOperationStatusCalculator;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceFavoriteService {

    private final PlaceService placeService;
    private final PlaceTranslationRepository placeTranslationRepository;
    private final PlaceRepository placeRepository;
    private final PlaceFavoriteStateRepository placeFavoriteStateRepository;
    private final PlaceFavoriteRepository placeFavoriteRepository;
    private final UserRepository userRepository;
    private final PlaceOperationStatusCalculator placeOperationStatusCalculator;
    private final Clock clock;

    @Transactional
    public void save(Long placeId, String userEmail) {
        placeService.getPlaceSummary(placeId, null);
        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        placeFavoriteStateRepository.save(placeId, user.getId());
    }

    @Transactional
    public void cancel(Long placeId, String userEmail) {
        Place place =
                placeRepository.findById(placeId).orElseThrow(() -> new GeneralException(PlaceErrorStatus.NOT_FOUND));
        if (place.isDeleted()) {
            throw new GeneralException(PlaceErrorStatus.NOT_FOUND);
        }
        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        placeFavoriteStateRepository.cancel(placeId, user.getId());
    }

    public FavoritePlacePage getFavorites(String userEmail, LocalDateTime lastFavoriteUpdatedAt, int size) {
        return getFavorites(userEmail, lastFavoriteUpdatedAt, size, LanguageCode.KO);
    }

    public FavoritePlacePage getFavorites(
            String userEmail, LocalDateTime lastFavoriteUpdatedAt, int size, LanguageCode language) {
        if (size < 1 || size > 20) {
            throw new GeneralException(PlaceErrorStatus.INVALID_REQUEST);
        }
        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);
        long totalCount = placeFavoriteRepository.countFavorites(user.getId(), now);
        List<FavoritePlaceCandidate> candidates =
                placeFavoriteStateRepository.findFavorites(user.getId(), lastFavoriteUpdatedAt);
        if (candidates.isEmpty()) {
            return new FavoritePlacePage(List.of(), totalCount, false, null);
        }

        List<Long> candidateIds =
                candidates.stream().map(FavoritePlaceCandidate::placeId).toList();
        LocalDate today = now.toLocalDate();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();
        DayOfWeek yesterdayDayOfWeek = today.minusDays(1).getDayOfWeek();
        List<DayOfWeek> relevantDayOfWeeks = List.of(todayDayOfWeek, yesterdayDayOfWeek);
        Map<Long, Place> placesById =
                placeRepository.findAllWithSchedulesByIdInAndDayOfWeekIn(candidateIds, relevantDayOfWeeks).stream()
                        .collect(Collectors.toMap(Place::getId, Function.identity()));
        List<FavoritePlaceCandidate> visibleCandidates = candidates.stream()
                .filter(candidate -> placesById.containsKey(candidate.placeId()))
                .filter(candidate -> {
                    Place place = placesById.get(candidate.placeId());
                    if (place.getType() != PlaceType.POPUP) {
                        return true;
                    }
                    PlaceOperationPeriod operationPeriod = place.getOperationPeriod();
                    LocalDateTime closingAt = operationPeriod.getClosingAt();
                    return now.isBefore(closingAt);
                })
                .limit(size + 1L)
                .toList();
        boolean hasNext = visibleCandidates.size() > size;
        List<FavoritePlaceCandidate> pageCandidates =
                visibleCandidates.stream().limit(size).toList();
        List<Long> pageIds =
                pageCandidates.stream().map(FavoritePlaceCandidate::placeId).toList();
        Map<Long, PlaceTranslation> translations = Map.of();
        if (language == LanguageCode.EN && !pageIds.isEmpty()) {
            translations = placeTranslationRepository.findAllByPlace_IdInAndLanguageCode(pageIds, language).stream()
                    .collect(Collectors.toMap(
                            translation -> translation.getPlace().getId(), Function.identity()));
        }
        Map<Long, PlaceTranslation> translationsById = translations;
        List<PlaceSummary> places = pageCandidates.stream()
                .map(candidate -> {
                    Place place = placesById.get(candidate.placeId());
                    PlaceOperationPeriod operationPeriod = place.getOperationPeriod();
                    List<PlaceDailySchedule> dailySchedules = place.dailySchedules(relevantDayOfWeeks);
                    PlaceType placeType = place.getType();
                    PlaceOperationState operationState =
                            placeOperationStatusCalculator.calculate(placeType, operationPeriod, dailySchedules, now);
                    return PlaceSummary.from(place, operationState, true)
                            .translated(translationsById.get(place.getId()));
                })
                .toList();
        if (!hasNext) {
            return new FavoritePlacePage(places, totalCount, false, null);
        }

        FavoritePlaceCandidate nextCursor = pageCandidates.get(pageCandidates.size() - 1);
        return new FavoritePlacePage(places, totalCount, true, nextCursor.favoriteUpdatedAt());
    }
}
