package com.fmi.domain.post.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
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

    @JsonCreator
    public static Radius from(int value) {
        for (Radius radius : values()) {
            if (radius.value == value) return radius;
        }
        throw new GeneralException(ErrorStatus._POST_RADIUS_NOT_MATCH);
    }

    @JsonValue
    public int toJson() {
        return value;
    }
}
