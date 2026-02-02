package com.fmi.domain.post.data;

import lombok.Getter;

@Getter
public enum Radius {
    DISTANCE_1000(1000),
    DISTANCE_3000(3000),
    DISTANCE_5000(5000);

    private final int value;

    Radius(int value) {
        this.value = value;
    }

}
