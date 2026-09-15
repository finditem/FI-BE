package com.fmi.domain.place.repository;

import com.fmi.domain.place.data.HomePlace;
import com.fmi.domain.place.data.enums.PlaceType;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaceRepositoryImpl implements PlaceRepositoryCustom {
    private static final String FIND_HOME_PLACES = """
            WITH selected_places AS (
                SELECT p.id, p.name, p.address, p.latitude, p.longitude,
                       p.station, p.station_distance_meters, p.type,
                       p.thumbnail_url, p.created_at, pop.start_date, pop.end_date
                FROM place p
                LEFT JOIN place_operation_period pop
                  ON pop.place_id = p.id AND pop.entity_status = 'ACTIVE'
                WHERE p.entity_status = 'ACTIVE'
                  AND (p.type <> 'POPUP' OR pop.closing_at > ?)
                ORDER BY p.created_at DESC, p.id DESC
                LIMIT 5
            )
            SELECT selected.id, selected.name, selected.address,
                   selected.latitude, selected.longitude, selected.station,
                   selected.station_distance_meters, selected.type,
                   selected.thumbnail_url, selected.start_date, selected.end_date,
                   pbh.day_of_week, pbh.is_closed,
                   pbh.type AS business_hour_type, pbh.start_time, pbh.end_time,
                   CASE WHEN pf.id IS NOT NULL THEN TRUE ELSE FALSE END AS is_favorite
            FROM selected_places selected
            JOIN place_business_hour pbh
              ON pbh.place_id = selected.id
             AND pbh.entity_status = 'ACTIVE'
             AND pbh.day_of_week IN (?, ?)
            LEFT JOIN place_favorite pf
              ON pf.user_id = ?
             AND pf.place_id = selected.id
             AND pf.is_favorite = TRUE
             AND pf.entity_status = 'ACTIVE'
            ORDER BY selected.created_at DESC, selected.id DESC, pbh.day_of_week, pbh.start_time
            """;

    private static final String FIND_HOME_PLACES_BY_TYPE = """
            WITH selected_places AS (
                SELECT p.id, p.name, p.address, p.latitude, p.longitude,
                       p.station, p.station_distance_meters, p.type,
                       p.thumbnail_url, p.created_at, pop.start_date, pop.end_date
                FROM place p
                LEFT JOIN place_operation_period pop
                  ON pop.place_id = p.id AND pop.entity_status = 'ACTIVE'
                WHERE p.entity_status = 'ACTIVE'
                  AND p.type = ?
                  AND (p.type <> 'POPUP' OR pop.closing_at > ?)
                ORDER BY p.created_at DESC, p.id DESC
                LIMIT 5
            )
            SELECT selected.id, selected.name, selected.address,
                   selected.latitude, selected.longitude, selected.station,
                   selected.station_distance_meters, selected.type,
                   selected.thumbnail_url, selected.start_date, selected.end_date,
                   pbh.day_of_week, pbh.is_closed,
                   pbh.type AS business_hour_type, pbh.start_time, pbh.end_time,
                   CASE WHEN pf.id IS NOT NULL THEN TRUE ELSE FALSE END AS is_favorite
            FROM selected_places selected
            JOIN place_business_hour pbh
              ON pbh.place_id = selected.id
             AND pbh.entity_status = 'ACTIVE'
             AND pbh.day_of_week IN (?, ?)
            LEFT JOIN place_favorite pf
              ON pf.user_id = ?
             AND pf.place_id = selected.id
             AND pf.is_favorite = TRUE
             AND pf.entity_status = 'ACTIVE'
            ORDER BY selected.created_at DESC, selected.id DESC, pbh.day_of_week, pbh.start_time
            """;

    private static final String FIND_MAP_CANDIDATE_IDS = """
            SELECT id
            FROM place
            WHERE entity_status = 'ACTIVE'
              AND type = ?
              AND latitude BETWEEN ? AND ?
              AND longitude BETWEEN ? AND ?
            ORDER BY ST_Distance_Sphere(
                        POINT(longitude, latitude),
                        POINT(?, ?)
                     ) ASC,
                     id DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<HomePlace> findHomePlaces(LocalDateTime now, DayOfWeek today, DayOfWeek yesterday, Long userId) {
        return jdbcTemplate.query(
                FIND_HOME_PLACES, new HomePlaceResultSetExtractor(), now, today.name(), yesterday.name(), userId);
    }

    @Override
    public List<HomePlace> findHomePlaces(
            PlaceType type, LocalDateTime now, DayOfWeek today, DayOfWeek yesterday, Long userId) {
        return jdbcTemplate.query(
                FIND_HOME_PLACES_BY_TYPE,
                new HomePlaceResultSetExtractor(),
                type.name(),
                now,
                today.name(),
                yesterday.name(),
                userId);
    }

    @Override
    public List<Long> findMapCandidateIds(
            PlaceType type,
            double latitude,
            double longitude,
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude) {
        return jdbcTemplate.queryForList(
                FIND_MAP_CANDIDATE_IDS,
                Long.class,
                type.name(),
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude,
                longitude,
                latitude);
    }
}
