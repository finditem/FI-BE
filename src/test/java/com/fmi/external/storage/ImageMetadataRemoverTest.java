package com.fmi.external.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ImageMetadataRemover")
class ImageMetadataRemoverTest {

    private static final String GPS_TEXT = "GPS 37.5665N 126.9780E";

    // Orientation=6과 GPS 문자열을 담은 EXIF APP1 segment
    private static final byte[] EXIF_WITH_ORIENTATION_6 = ByteBuffer.allocate(
                    4 + 6 + 8 + 2 + 12 + 4 + GPS_TEXT.length())
            .put(new byte[] {(byte) 0xFF, (byte) 0xE1})
            .putShort((short) (2 + 6 + 8 + 2 + 12 + 4 + GPS_TEXT.length()))
            .put("Exif\0\0".getBytes(StandardCharsets.US_ASCII))
            .put(new byte[] {'M', 'M', 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08})
            .put(new byte[] {0x00, 0x01})
            .put(new byte[] {0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x06, 0x00, 0x00})
            .put(new byte[] {0x00, 0x00, 0x00, 0x00})
            .put(GPS_TEXT.getBytes(StandardCharsets.US_ASCII))
            .array();

    // 결과에 들어가야 하는 Orientation=6 전용 EXIF APP1 segment
    private static final byte[] ORIENTATION_ONLY_EXIF_6 = {
        (byte) 0xFF,
        (byte) 0xE1,
        0x00,
        0x22,
        'E',
        'x',
        'i',
        'f',
        0x00,
        0x00,
        'M',
        'M',
        0x00,
        0x2A,
        0x00,
        0x00,
        0x00,
        0x08,
        0x00,
        0x01,
        0x01,
        0x12,
        0x00,
        0x03,
        0x00,
        0x00,
        0x00,
        0x01,
        0x00,
        0x06,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00
    };

    private BufferedImage sourceImage;
    private byte[] plainJpeg;
    private byte[] plainPng;

    @BeforeEach
    void setUp() throws Exception {
        sourceImage = new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                sourceImage.setRGB(x, y, (x * 30) << 16 | (y * 60) << 8 | 0x80);
            }
        }
        ByteArrayOutputStream jpegOutput = new ByteArrayOutputStream();
        ImageIO.write(sourceImage, "jpg", jpegOutput);
        plainJpeg = jpegOutput.toByteArray();
        ByteArrayOutputStream pngOutput = new ByteArrayOutputStream();
        ImageIO.write(sourceImage, "png", pngOutput);
        plainPng = pngOutput.toByteArray();
    }

    @Nested
    @DisplayName("JPEG 메타데이터를 제거할 때")
    class DescribeRemoveFromJpeg {

        @Test
        @DisplayName("EXIF, XMP, IPTC, 주석과 이미지 뒤에 붙은 데이터를 제거한다")
        void itRemovesMetadataSegmentsAndTrailingData() throws Exception {
            // given
            byte[] xmpPayload =
                    "http://ns.adobe.com/xap/1.0/\0<xmp>xmp-location</xmp>".getBytes(StandardCharsets.UTF_8);
            byte[] iptcPayload = "Photoshop 3.0\0iptc-location".getBytes(StandardCharsets.UTF_8);
            byte[] commentPayload = "comment-location".getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            jpeg.write(plainJpeg, 0, 2);
            jpeg.write(EXIF_WITH_ORIENTATION_6);
            jpeg.write(new byte[] {(byte) 0xFF, (byte) 0xE1, 0x00, (byte) (xmpPayload.length + 2)});
            jpeg.write(xmpPayload);
            jpeg.write(new byte[] {(byte) 0xFF, (byte) 0xED, 0x00, (byte) (iptcPayload.length + 2)});
            jpeg.write(iptcPayload);
            jpeg.write(new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00, (byte) (commentPayload.length + 2)});
            jpeg.write(commentPayload);
            jpeg.write(plainJpeg, 2, plainJpeg.length - 2);
            jpeg.write("trailing-motion-video".getBytes(StandardCharsets.UTF_8));

            // when
            byte[] result = ImageMetadataRemover.remove(jpeg.toByteArray());

            // then
            String resultText = new String(result, StandardCharsets.ISO_8859_1);
            assertThat(resultText)
                    .doesNotContain(GPS_TEXT)
                    .doesNotContain("xmp-location")
                    .doesNotContain("iptc-location")
                    .doesNotContain("comment-location")
                    .doesNotContain("trailing-motion-video");
            assertThat(Arrays.copyOfRange(result, result.length - 2, result.length))
                    .containsExactly(0xFF, 0xD9);
        }

        @Test
        @DisplayName("픽셀 데이터를 다시 인코딩하지 않고 그대로 유지한다")
        void itKeepsPixelsUnchanged() throws Exception {
            // given
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            jpeg.write(plainJpeg, 0, 2);
            jpeg.write(EXIF_WITH_ORIENTATION_6);
            jpeg.write(plainJpeg, 2, plainJpeg.length - 2);
            BufferedImage expected = ImageIO.read(new ByteArrayInputStream(plainJpeg));

            // when
            byte[] result = ImageMetadataRemover.remove(jpeg.toByteArray());

            // then
            BufferedImage actual = ImageIO.read(new ByteArrayInputStream(result));
            assertThat(actual.getRGB(0, 0, 8, 4, null, 0, 8)).containsExactly(expected.getRGB(0, 0, 8, 4, null, 0, 8));
        }

        @Test
        @DisplayName("ICC 색상 프로필 segment를 유지한다")
        void itKeepsIccProfileSegment() throws Exception {
            // given
            byte[] iccPayload = "ICC_PROFILE\0\1\1icc-profile-data".getBytes(StandardCharsets.US_ASCII);
            byte[] iccSegment = ByteBuffer.allocate(iccPayload.length + 4)
                    .put(new byte[] {(byte) 0xFF, (byte) 0xE2})
                    .putShort((short) (iccPayload.length + 2))
                    .put(iccPayload)
                    .array();
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            jpeg.write(plainJpeg, 0, 2);
            jpeg.write(iccSegment);
            jpeg.write(plainJpeg, 2, plainJpeg.length - 2);

            // when
            byte[] result = ImageMetadataRemover.remove(jpeg.toByteArray());

            // then
            assertThat(new String(result, StandardCharsets.ISO_8859_1))
                    .contains(new String(iccSegment, StandardCharsets.ISO_8859_1));
        }

        @Nested
        @DisplayName("EXIF에 회전 정보가 있으면")
        class ContextWithOrientation {

            @Test
            @DisplayName("Orientation 태그 하나만 담은 EXIF로 바꾼다")
            void itKeepsOnlyOrientation() throws Exception {
                // given
                ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
                jpeg.write(plainJpeg, 0, 2);
                jpeg.write(EXIF_WITH_ORIENTATION_6);
                jpeg.write(plainJpeg, 2, plainJpeg.length - 2);

                // when
                byte[] result = ImageMetadataRemover.remove(jpeg.toByteArray());

                // then
                String resultText = new String(result, StandardCharsets.ISO_8859_1);
                assertThat(resultText).contains(new String(ORIENTATION_ONLY_EXIF_6, StandardCharsets.ISO_8859_1));
                assertThat(resultText.indexOf("Exif\0\0")).isEqualTo(resultText.lastIndexOf("Exif\0\0"));
            }
        }

        @Nested
        @DisplayName("EXIF에 회전 정보가 없으면")
        class ContextWithoutOrientation {

            @Test
            @DisplayName("EXIF를 남기지 않는다")
            void itRemovesExifCompletely() throws Exception {
                // given
                byte[] exifPayload = ("Exif\0\0" + GPS_TEXT).getBytes(StandardCharsets.US_ASCII);
                ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
                jpeg.write(plainJpeg, 0, 2);
                jpeg.write(new byte[] {(byte) 0xFF, (byte) 0xE1, 0x00, (byte) (exifPayload.length + 2)});
                jpeg.write(exifPayload);
                jpeg.write(plainJpeg, 2, plainJpeg.length - 2);

                // when
                byte[] result = ImageMetadataRemover.remove(jpeg.toByteArray());

                // then
                assertThat(new String(result, StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
            }
        }
    }

    @Nested
    @DisplayName("PNG 메타데이터를 제거할 때")
    class DescribeRemoveFromPng {

        @Test
        @DisplayName("텍스트, eXIf, tIME chunk와 이미지 뒤에 붙은 데이터를 제거하고 픽셀은 유지한다")
        void itRemovesMetadataChunksAndKeepsPixels() throws Exception {
            // given
            byte[] textData = "Comment\0text-location".getBytes(StandardCharsets.ISO_8859_1);
            byte[] exifData = GPS_TEXT.getBytes(StandardCharsets.ISO_8859_1);
            byte[] timeData = {0x07, (byte) 0xEA, 0x0B, 0x06, 0x0C, 0x00, 0x00};
            ByteArrayOutputStream metadataChunks = new ByteArrayOutputStream();
            for (Object[] chunk : new Object[][] {{"tEXt", textData}, {"eXIf", exifData}, {"tIME", timeData}}) {
                byte[] type = ((String) chunk[0]).getBytes(StandardCharsets.US_ASCII);
                byte[] data = (byte[]) chunk[1];
                CRC32 crc = new CRC32();
                crc.update(type);
                crc.update(data);
                metadataChunks.write(ByteBuffer.allocate(4).putInt(data.length).array());
                metadataChunks.write(type);
                metadataChunks.write(data);
                metadataChunks.write(
                        ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
            }
            int afterIhdr = 8 + 12 + 13;
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            png.write(plainPng, 0, afterIhdr);
            png.write(metadataChunks.toByteArray());
            png.write(plainPng, afterIhdr, plainPng.length - afterIhdr);
            png.write("trailing-data".getBytes(StandardCharsets.US_ASCII));

            // when
            byte[] result = ImageMetadataRemover.remove(png.toByteArray());

            // then
            assertThat(result).containsExactly(plainPng);
            BufferedImage actual = ImageIO.read(new ByteArrayInputStream(result));
            assertThat(actual.getRGB(0, 0, 8, 4, null, 0, 8))
                    .containsExactly(sourceImage.getRGB(0, 0, 8, 4, null, 0, 8));
        }
    }

    @Nested
    @DisplayName("JPEG나 PNG가 아닌 파일의 메타데이터를 제거할 때")
    class DescribeRemoveFromUnsupportedFormat {

        @Test
        @DisplayName("IllegalArgumentException으로 거절한다")
        void itThrowsIllegalArgumentException() {
            // given
            byte[] notImage = "<html>not image</html>".getBytes(StandardCharsets.UTF_8);

            // when & then
            assertThatThrownBy(() -> ImageMetadataRemover.remove(notImage))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
