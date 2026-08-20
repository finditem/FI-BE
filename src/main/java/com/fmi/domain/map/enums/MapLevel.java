package com.fmi.domain.map.enums;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Arrays;
import lombok.Getter;

public enum MapLevel {
    LEVEL_1(1, 100, 150),
    LEVEL_2(2, 200, 300),
    LEVEL_3(3, 300, 450),
    LEVEL_4(4, 500, 750),
    LEVEL_5(5, 800, 1_000),
    LEVEL_6(6, 1_500, 1_700),
    LEVEL_7(7, 2_000, 2_300),
    LEVEL_8(8, 3_000, 3_100);

    private final int level;

    @Getter
    private final int halfWidthMeter;

    @Getter
    private final int halfHeightMeter;

    MapLevel(int level, int halfWidthMeter, int halfHeightMeter) {
        this.level = level;
        this.halfWidthMeter = halfWidthMeter;
        this.halfHeightMeter = halfHeightMeter;
    }

    public static MapLevel from(int level) {
        return Arrays.stream(values())
                .filter(l -> l.level == level)
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus._MAP_LEVEL_INVALID));
    }
}
