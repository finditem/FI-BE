package com.fmi.domain.temperature.data;

import lombok.Getter;

@Getter
public enum TemperatureActivityType {
    FOUND_POST(5),
    RETURN_COMPLETED(20),
    REVIEW_RECEIVED(10);

    // 0.1℃ 단위
    private final int deltaTenths;

    TemperatureActivityType(int deltaTenths) {
        this.deltaTenths = deltaTenths;
    }
}
