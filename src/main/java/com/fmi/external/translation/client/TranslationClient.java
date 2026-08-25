package com.fmi.external.translation.client;

import com.fmi.domain.Enum.LanguageCode;

public interface TranslationClient {
    default String translate(String text, LanguageCode targetLang) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return doTranslate(text, targetLang);
    }

    String doTranslate(String text, LanguageCode targetLang);
}
