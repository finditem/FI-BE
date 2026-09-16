package com.fmi.domain.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.place.data.Place;
import com.fmi.domain.place.data.PlaceBusinessHour;
import com.fmi.domain.place.data.PlaceTimeRange;
import com.fmi.domain.place.data.enums.PlaceBusinessHourType;
import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("PlaceRepository")
class PlaceRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceBusinessHourRepository placeBusinessHourRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        placeBusinessHourRepository.deleteAll();
        placeRepository.deleteAll();
    }

    @Nested
    @DisplayName("요일을 지정해 장소와 영업시간을 조회할 때")
    class DescribeFindAllWithSchedulesByIdInAndDayOfWeekIn {

        @Test
        @DisplayName("지정한 요일의 활성 영업시간만 조회한다")
        void itFetchesOnlyActiveBusinessHoursForRequestedDays() {
            // given
            Place place = Place.builder()
                    .name("성수 카페")
                    .address("서울 성동구")
                    .latitude(37.54)
                    .longitude(127.05)
                    .station("성수역")
                    .stationDistanceMeters(100)
                    .type(PlaceType.CAFE)
                    .thumbnailUrl("thumbnail-url")
                    .build();
            Arrays.stream(DayOfWeek.values()).forEach(dayOfWeek -> {
                PlaceTimeRange timeRange =
                        new PlaceTimeRange(PlaceBusinessHourType.BUSINESS, LocalTime.of(10, 0), LocalTime.of(22, 0));
                PlaceBusinessHour businessHour = PlaceBusinessHour.builder()
                        .dayOfWeek(dayOfWeek)
                        .closed(false)
                        .timeRange(timeRange)
                        .build();
                place.addBusinessHour(businessHour);
            });
            Place savedPlace = placeRepository.saveAndFlush(place);
            Long placeId = savedPlace.getId();
            entityManager.clear();
            List<Long> placeIds = List.of(placeId);
            List<DayOfWeek> dayOfWeeks = List.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY);

            // when
            List<Place> places = placeRepository.findAllWithSchedulesByIdInAndDayOfWeekIn(placeIds, dayOfWeeks);

            // then
            assertThat(places).singleElement().satisfies(foundPlace -> assertThat(foundPlace.getBusinessHours())
                    .extracting(PlaceBusinessHour::getDayOfWeek)
                    .containsExactlyInAnyOrder(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY));
        }
    }
}
