package com.fmi.domain.place.repository;

import com.fmi.domain.place.data.HomePlace;
import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.user.user.data.enums.LanguageCode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaceHomeJdbcRepository implements PlaceHomeRepository {

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
            SELECT selected.id, COALESCE(pt.translated_name, selected.name) AS name, COALESCE(pt.translated_address, selected.address) AS address,
                   selected.latitude, selected.longitude, COALESCE(pt.translated_station, selected.station) AS station,
                   selected.station_distance_meters, selected.type,
                   selected.thumbnail_url, selected.start_date, selected.end_date,
                   pbh.day_of_week, pbh.is_closed,
                   pbh.type AS business_hour_type, pbh.start_time, pbh.end_time,
                   CASE WHEN pf.id IS NOT NULL THEN TRUE ELSE FALSE END AS is_favorite
            FROM selected_places selected
            LEFT JOIN place_translation pt ON pt.place_id = selected.id AND pt.language_code = ?
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
            SELECT selected.id, COALESCE(pt.translated_name, selected.name) AS name, COALESCE(pt.translated_address, selected.address) AS address,
                   selected.latitude, selected.longitude, COALESCE(pt.translated_station, selected.station) AS station,
                   selected.station_distance_meters, selected.type,
                   selected.thumbnail_url, selected.start_date, selected.end_date,
                   pbh.day_of_week, pbh.is_closed,
                   pbh.type AS business_hour_type, pbh.start_time, pbh.end_time,
                   CASE WHEN pf.id IS NOT NULL THEN TRUE ELSE FALSE END AS is_favorite
            FROM selected_places selected
            LEFT JOIN place_translation pt ON pt.place_id = selected.id AND pt.language_code = ?
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

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<HomePlace> findHomePlaces(
            LocalDateTime nowInUtc, DayOfWeek today, DayOfWeek yesterday, Long userId, LanguageCode language) {
        return jdbcTemplate.query(
                FIND_HOME_PLACES,
                new HomePlaceResultSetExtractor(),
                nowInUtc,
                language.name(),
                today.name(),
                yesterday.name(),
                userId);
    }

    @Override
    public List<HomePlace> findHomePlaces(
            PlaceType type,
            LocalDateTime nowInUtc,
            DayOfWeek today,
            DayOfWeek yesterday,
            Long userId,
            LanguageCode language) {
        return jdbcTemplate.query(
                FIND_HOME_PLACES_BY_TYPE,
                new HomePlaceResultSetExtractor(),
                type.name(),
                nowInUtc,
                language.name(),
                today.name(),
                yesterday.name(),
                userId);
    }
}
