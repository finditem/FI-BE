package com.fmi.external.translation.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.Enum.LanguageCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TranslationClientTest {

    private final List<String> requestedTexts = new ArrayList<>();

    private final TranslationClient client = (text, targetLang) -> {
        requestedTexts.add(text);
        return "translated:" + text;
    };

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\n", "\t"})
    @DisplayName("null이거나 공백뿐인 텍스트는 번역 API를 호출하지 않고 그대로 반환한다")
    void skipsTranslationForNullOrBlankText(String text) {
        assertThat(client.translate(text, LanguageCode.EN)).isEqualTo(text);
        assertThat(requestedTexts).isEmpty();
    }

    @Test
    @DisplayName("내용이 있는 텍스트는 실제 번역 호출로 위임한다")
    void delegatesToDoTranslateForNonBlankText() {
        String result = client.translate("지갑", LanguageCode.EN);

        assertThat(result).isEqualTo("translated:지갑");
        assertThat(requestedTexts).containsExactly("지갑");
    }
}
