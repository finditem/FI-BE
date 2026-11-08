package com.fmi.external.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ImageFormat")
class ImageFormatTest {

    @Nested
    @DisplayName("파일 내용으로 형식을 판별할 때")
    class DescribeDetect {

        @Test
        @DisplayName("JPEG 시그니처면 JPEG로 판별한다")
        void itDetectsJpeg() {
            // given
            byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

            // when & then
            assertThat(ImageFormat.detect(jpegHeader)).contains(ImageFormat.JPEG);
        }

        @Test
        @DisplayName("PNG 시그니처면 PNG로 판별한다")
        void itDetectsPng() {
            // given
            byte[] pngHeader = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0x00};

            // when & then
            assertThat(ImageFormat.detect(pngHeader)).contains(ImageFormat.PNG);
        }

        @Test
        @DisplayName("JPEG나 PNG 시그니처가 아니면 형식을 판별하지 않는다")
        void itReturnsEmptyForOtherContent() {
            // given
            byte[] html = "<html>not image</html>".getBytes(StandardCharsets.UTF_8);

            // when & then
            assertThat(ImageFormat.detect(html)).isEmpty();
        }

        @Test
        @DisplayName("시그니처보다 짧으면 형식을 판별하지 않는다")
        void itReturnsEmptyForShortContent() {
            // given
            byte[] tooShort = {(byte) 0xFF, (byte) 0xD8};

            // when & then
            assertThat(ImageFormat.detect(tooShort)).isEmpty();
        }
    }
}
