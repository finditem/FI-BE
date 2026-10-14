package com.fmi.domain.temperature.data;

import java.math.BigDecimal;

// 0.1℃ 단위 정수로 다뤄 소수 오차를 피한다.
public record Temperature(int tenths) {

    public static final int INITIAL_TENTHS = 365;
    public static final int MIN_TENTHS = 0;
    public static final int MAX_TENTHS = 990;

    public static Temperature initial() {
        return new Temperature(INITIAL_TENTHS);
    }

    public BigDecimal celsius() {
        return BigDecimal.valueOf(tenths, 1);
    }
}
