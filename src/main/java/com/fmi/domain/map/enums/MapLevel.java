package com.fmi.domain.map.enums;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.Getter;

import java.util.Arrays;

public enum MapLevel {

    LEVEL_1(1, 50),
    LEVEL_2(2, 100),
    LEVEL_3(3, 200),
    LEVEL_4(4, 400),
    LEVEL_5(5, 800),
    LEVEL_6(6, 1_500),
    LEVEL_7(7, 3_000),
    LEVEL_8(8, 6_000),
    LEVEL_9(9, 12_000),
    LEVEL_10(10, 25_000),
    LEVEL_11(11, 50_000);

    private final int level;

    @Getter
    private final int radiusMeter;

    MapLevel(int level, int radiusMeter) {
        this.level = level;
        this.radiusMeter = radiusMeter;
    }

    public static MapLevel from(int level) {
        return Arrays.stream(values())
                .filter(l -> l.level == level)
                .findFirst()
                .orElseThrow(() ->
                        new GeneralException(ErrorStatus._MAP_LEVEL_INVALID));
    }
}