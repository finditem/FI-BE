package com.fmi.domain.map.enums;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.Getter;

import java.util.Arrays;

public enum MapLevel {

    LEVEL_1(1, 20),
    LEVEL_2(2, 30),
    LEVEL_3(3, 50),
    LEVEL_4(4, 100),
    LEVEL_5(5, 250),
    LEVEL_6(6, 500),
    LEVEL_7(7, 1_000),
    LEVEL_8(8, 2_000),
    LEVEL_9(9, 4_000),
    LEVEL_10(10, 8_000),
    LEVEL_11(11, 16_000);

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