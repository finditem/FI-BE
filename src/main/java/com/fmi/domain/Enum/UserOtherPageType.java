package com.fmi.domain.Enum;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum UserOtherPageType {
    POSTS("posts"),
    COMMENTS("comments"),
    FAVORITES("favorites");

    private final String paramValue;

    UserOtherPageType(String paramValue) {
        this.paramValue = paramValue;
    }

    public static UserOtherPageType fromParam(String value) {
        if (value == null || value.isBlank()) {
            return POSTS;
        }
        String normalized = value.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(type ->
                        type.paramValue.equals(normalized) || type.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_PAGE_TYPE_INVALID));
    }
}
