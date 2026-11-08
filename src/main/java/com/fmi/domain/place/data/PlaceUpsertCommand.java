package com.fmi.domain.place.data;

import com.fmi.domain.place.data.enums.PlaceType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record PlaceUpsertCommand(
        String name,
        String address,
        double latitude,
        double longitude,
        String station,
        int stationDistanceMeters,
        PlaceType type,
        LocalDate operationStartDate,
        LocalDate operationEndDate,
        List<PlaceDailySchedule> dailySchedules,
        String nameEn,
        String addressEn,
        String stationEn) {

    public PlaceUpsertCommand(
            String name,
            String address,
            double latitude,
            double longitude,
            String station,
            int stationDistanceMeters,
            PlaceType type,
            LocalDate operationStartDate,
            LocalDate operationEndDate,
            List<PlaceDailySchedule> dailySchedules) {
        this(
                name,
                address,
                latitude,
                longitude,
                station,
                stationDistanceMeters,
                type,
                operationStartDate,
                operationEndDate,
                dailySchedules,
                null,
                null,
                null);
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    public PlaceUpsertCommand {
        nameEn = normalize(nameEn);
        addressEn = normalize(addressEn);
        stationEn = normalize(stationEn);
        if (dailySchedules != null) {
            dailySchedules = Collections.unmodifiableList(new ArrayList<>(dailySchedules));
        }
    }
}
