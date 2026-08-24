package com.fmi.external.translation.service;

import com.fmi.domain.Enum.LanguageCode;
import com.fmi.external.translation.client.TranslationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final TranslationClient translationClient;

    public String translateText(String text, LanguageCode targetLang) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return translationClient.translate(text, targetLang);
    }
}
