package com.fmi.external.translation.client;

import com.fmi.domain.Enum.LanguageCode;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeepLTranslationClient implements TranslationClient {

    private final DeepLProperties deepLProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String translate(String text, LanguageCode targetLang) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "DeepL-Auth-Key " + deepLProperties.apiKey());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("text", text);
        params.add("target_lang", toDeepLCode(targetLang));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            DeepLResponse response =
                    restTemplate.postForObject(deepLProperties.baseUrl(), request, DeepLResponse.class);

            if (response == null || response.translations == null || response.translations.isEmpty()) {
                throw new GeneralException(ErrorStatus._TRANSLATION_API_ERROR);
            }

            return response.translations.get(0).text;

        } catch (RestClientException e) {
            log.error("DeepL 번역 요청 실패", e);
            throw new GeneralException(ErrorStatus._TRANSLATION_API_ERROR);
        }
    }

    private String toDeepLCode(LanguageCode languageCode) {
        return switch (languageCode) {
            case KO -> "KO";
            case EN -> "EN-US";
        };
    }

    @Data
    private static class DeepLResponse {
        private List<Translation> translations;
    }

    @Data
    private static class Translation {
        private String text;
    }
}
