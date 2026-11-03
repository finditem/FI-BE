package com.fmi.domain.place.web;

import com.fmi.domain.place.exception.PlaceErrorStatus;
import com.fmi.domain.user.user.data.enums.LanguageCode;
import com.fmi.global.apiPayload.exception.GeneralException;

public final class PlaceLanguageResolver {
    private PlaceLanguageResolver() {}

    public static LanguageCode resolve(String language) {
        if (language == null) {
            return LanguageCode.KO;
        }
        try {
            return LanguageCode.valueOf(language);
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(PlaceErrorStatus.INVALID_REQUEST);
        }
    }
}
