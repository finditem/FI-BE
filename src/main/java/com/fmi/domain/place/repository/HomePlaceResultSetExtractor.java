package com.fmi.domain.place.repository;

import com.fmi.domain.place.data.HomePlace;
import com.fmi.domain.place.data.PlaceDailySchedule;
import com.fmi.domain.place.data.PlaceTimeRange;
import com.fmi.domain.place.data.enums.PlaceBusinessHourType;
import com.fmi.domain.place.data.enums.PlaceType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

final class HomePlaceResultSetExtractor implements ResultSetExtractor<List<HomePlace>> {
    @Override
    public List<HomePlace> extractData(ResultSet resultSet) throws SQLException, DataAccessException {
        Map<Long, Builder> places = new LinkedHashMap<>();
        while (resultSet.next()) {
            long placeId = resultSet.getLong("id");
            Builder place = places.computeIfAbsent(placeId, ignored -> createBuilder(resultSet));
            place.addSchedule(
                    DayOfWeek.valueOf(resultSet.getString("day_of_week")),
                    resultSet.getBoolean("is_closed"),
                    new PlaceTimeRange(
                            PlaceBusinessHourType.valueOf(resultSet.getString("business_hour_type")),
                            resultSet.getObject("start_time", LocalTime.class),
                            resultSet.getObject("end_time", LocalTime.class)));
        }
        return places.values().stream().map(Builder::build).toList();
    }

    private Builder createBuilder(ResultSet resultSet) {
        try {
            return new Builder(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getString("address"),
                    resultSet.getDouble("latitude"),
                    resultSet.getDouble("longitude"),
                    resultSet.getString("station"),
                    resultSet.getInt("station_distance_meters"),
                    PlaceType.valueOf(resultSet.getString("type")),
                    resultSet.getString("thumbnail_url"),
                    resultSet.getObject("start_date", LocalDate.class),
                    resultSet.getObject("end_date", LocalDate.class),
                    resultSet.getBoolean("is_favorite"));
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to map home place", exception);
        }
    }

    private static final class Builder {
        private final Long id;
        private final String name;
        private final String address;
        private final double latitude;
        private final double longitude;
        private final String station;
        private final int stationDistanceMeters;
        private final PlaceType type;
        private final String thumbnailUrl;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final boolean favorite;
        private final Map<DayOfWeek, ScheduleBuilder> schedules = new LinkedHashMap<>();

        private Builder(
                Long id,
                String name,
                String address,
                double latitude,
                double longitude,
                String station,
                int stationDistanceMeters,
                PlaceType type,
                String thumbnailUrl,
                LocalDate startDate,
                LocalDate endDate,
                boolean favorite) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
            this.station = station;
            this.stationDistanceMeters = stationDistanceMeters;
            this.type = type;
            this.thumbnailUrl = thumbnailUrl;
            this.startDate = startDate;
            this.endDate = endDate;
            this.favorite = favorite;
        }

        private void addSchedule(DayOfWeek dayOfWeek, boolean closed, PlaceTimeRange timeRange) {
            schedules
                    .computeIfAbsent(dayOfWeek, ignored -> new ScheduleBuilder(closed))
                    .ranges
                    .add(timeRange);
        }

        private HomePlace build() {
            List<PlaceDailySchedule> dailySchedules = schedules.entrySet().stream()
                    .map(entry ->
                            new PlaceDailySchedule(entry.getKey(), entry.getValue().closed, entry.getValue().ranges))
                    .toList();
            return new HomePlace(
                    id,
                    name,
                    address,
                    latitude,
                    longitude,
                    station,
                    stationDistanceMeters,
                    type,
                    thumbnailUrl,
                    startDate,
                    endDate,
                    dailySchedules,
                    favorite);
        }
    }

    private static final class ScheduleBuilder {
        private final boolean closed;
        private final List<PlaceTimeRange> ranges = new ArrayList<>();

        private ScheduleBuilder(boolean closed) {
            this.closed = closed;
        }
    }
}
