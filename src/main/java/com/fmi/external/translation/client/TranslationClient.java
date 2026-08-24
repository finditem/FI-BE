package com.fmi.external.translation.client;

import com.fmi.domain.Enum.LanguageCode;

public interface TranslationClient {
    String translate(String text, LanguageCode targetLang);
}
