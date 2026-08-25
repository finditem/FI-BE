package com.fmi.external.translation.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TranslationTextNormalizerTest {

    @Test
    @DisplayName("null은 빈 문자열로 정규화된다")
    void normalizeNullToEmptyString() {
        assertThat(TranslationTextNormalizer.normalize(null)).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @DisplayName("특수문자를 제거하고 연속 공백을 하나로 합친 뒤 앞뒤를 자른다")
    @CsvSource(
            delimiter = '|',
            value = {
                "'  지갑을   잃어버렸어요!!! '|지갑을 잃어버렸어요",
                "'지갑을\t잃어버렸어요'|지갑을 잃어버렸어요",
                "'Lost my wallet, near exit 2.'|Lost my wallet near exit 2",
                "'@#$%^&*()'|''",
                "''|''"
            })
    void normalizeRemovesSymbolsAndCollapsesWhitespace(String input, String expected) {
        assertThat(TranslationTextNormalizer.normalize(input)).isEqualTo(expected == null ? "" : expected);
    }

    @Test
    @DisplayName("한글·영문·숫자는 보존된다")
    void keepsHangulAlphabetAndDigits() {
        assertThat(TranslationTextNormalizer.normalize("강남역 2번 출구 exit2")).isEqualTo("강남역 2번 출구 exit2");
    }

    @Test
    @DisplayName("줄바꿈만 다른 텍스트는 같은 값으로 정규화된다")
    void normalizesLineBreaksToSingleSpace() {
        String withLineBreaks = "강남역 2번 출구\n\n근처";
        String withSpaces = "강남역 2번 출구 근처";

        assertThat(TranslationTextNormalizer.normalize(withLineBreaks))
                .isEqualTo(TranslationTextNormalizer.normalize(withSpaces));
    }

    @Test
    @DisplayName("같은 입력은 항상 같은 해시를 만든다")
    void hashIsDeterministic() {
        String text = "지갑을 잃어버렸어요";

        assertThat(TranslationTextNormalizer.hash(text)).isEqualTo(TranslationTextNormalizer.hash(text));
    }

    @Test
    @DisplayName("해시는 64자리 소문자 16진수(SHA-256)이다")
    void hashIsSha256Hex() {
        String hash = TranslationTextNormalizer.hash("지갑을 잃어버렸어요");

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("빈 문자열의 해시는 SHA-256 표준 값과 같다")
    void hashOfEmptyStringMatchesSha256Constant() {
        assertThat(TranslationTextNormalizer.hash(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("서로 다른 텍스트는 다른 해시를 만든다")
    void differentTextProducesDifferentHash() {
        assertThat(TranslationTextNormalizer.hash("지갑을 잃어버렸어요"))
                .isNotEqualTo(TranslationTextNormalizer.hash("가방을 잃어버렸어요"));
    }
}
