package com.fmi.external.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.dto.UploadedImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Testcontainers
@DisplayName("S3Service")
class S3ServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String GPS_TEXT = "GPS 37.5665N 126.9780E";

    @Container
    private static final GenericContainer<?> S3_MOCK = new GenericContainer<>("adobe/s3mock:3.12.0")
            .withEnv("initialBuckets", BUCKET)
            .withExposedPorts(9090)
            .waitingFor(Wait.forLogMessage(".*Started S3MockApplication.*", 1));

    private S3Client s3Client;
    private S3Service s3Service;
    private byte[] jpegWithGps;
    private byte[] plainPng;

    @BeforeEach
    void setUp() throws Exception {
        s3Client = S3Client.builder()
                .endpointOverride(URI.create("http://" + S3_MOCK.getHost() + ":" + S3_MOCK.getMappedPort(9090)))
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
                .forcePathStyle(true)
                .build();
        s3Service = new S3Service(s3Client);
        ReflectionTestUtils.setField(s3Service, "bucketName", BUCKET);

        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpegOutput = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", jpegOutput);
        byte[] plainJpeg = jpegOutput.toByteArray();
        byte[] exifPayload = ("Exif\0\0" + GPS_TEXT).getBytes(StandardCharsets.US_ASCII);
        jpegWithGps = ByteBuffer.allocate(plainJpeg.length + exifPayload.length + 4)
                .put(plainJpeg, 0, 2)
                .put(new byte[] {(byte) 0xFF, (byte) 0xE1, 0x00, (byte) (exifPayload.length + 2)})
                .put(exifPayload)
                .put(plainJpeg, 2, plainJpeg.length - 2)
                .array();

        ByteArrayOutputStream pngOutput = new ByteArrayOutputStream();
        ImageIO.write(image, "png", pngOutput);
        plainPng = pngOutput.toByteArray();
    }

    @Nested
    @DisplayName("이미지를 업로드할 때")
    class DescribeUpload {

        @Nested
        @DisplayName("GPS 정보가 담긴 JPEG이면")
        class ContextWithGpsJpeg {

            @Test
            @DisplayName("메타데이터를 제거하고 image/jpeg로 저장한다")
            void itStoresJpegWithoutMetadata() {
                // given
                MockMultipartFile file = new MockMultipartFile("image", "home-photo.jpg", "image/jpeg", jpegWithGps);

                // when
                String url = s3Service.upload(List.of(file)).get(0);

                // then
                String key = url.substring(url.indexOf("/" + BUCKET + "/") + BUCKET.length() + 2);
                ResponseBytes<GetObjectResponse> stored = s3Client.getObjectAsBytes(
                        request -> request.bucket(BUCKET).key(key));
                assertThat(stored.asString(StandardCharsets.ISO_8859_1)).doesNotContain(GPS_TEXT);
                assertThat(stored.response().contentType()).isEqualTo("image/jpeg");
                assertThat(key).matches("[0-9a-f-]{36}\\.jpg");
            }
        }

        @Nested
        @DisplayName("확장자와 실제 형식이 다르면")
        class ContextWithMismatchedExtension {

            @Test
            @DisplayName("파일 내용의 형식으로 key 확장자와 Content-Type을 정한다")
            void itUsesDetectedFormat() {
                // given
                MockMultipartFile file = new MockMultipartFile("image", "screenshot.JPG", "image/jpeg", plainPng);

                // when
                String url = s3Service.upload(List.of(file)).get(0);

                // then
                String key = url.substring(url.indexOf("/" + BUCKET + "/") + BUCKET.length() + 2);
                String contentType = s3Client.headObject(
                                request -> request.bucket(BUCKET).key(key))
                        .contentType();
                assertThat(key).endsWith(".png");
                assertThat(contentType).isEqualTo("image/png");
            }
        }

        @Nested
        @DisplayName("허용된 확장자지만 이미지가 아니면")
        class ContextWithNonImageContent {

            @Test
            @DisplayName("FILE415-EXT_UNSUPPORTED로 거절하고 저장하지 않는다")
            void itRejectsWithoutStoring() {
                // given
                MockMultipartFile file = new MockMultipartFile(
                        "image", "fake.jpg", "image/jpeg", "<html>not image</html>".getBytes(StandardCharsets.UTF_8));
                int objectCountBefore = s3Client.listObjectsV2(request -> request.bucket(BUCKET))
                        .contents()
                        .size();

                // when & then
                assertThatThrownBy(() -> s3Service.upload(List.of(file)))
                        .isInstanceOf(GeneralException.class)
                        .extracting(exception -> ((GeneralException) exception).getCode())
                        .isEqualTo(ErrorStatus._INVALID_FILE_EXTENSION);
                int objectCountAfter = s3Client.listObjectsV2(request -> request.bucket(BUCKET))
                        .contents()
                        .size();
                assertThat(objectCountAfter).isEqualTo(objectCountBefore);
            }
        }

        @Nested
        @DisplayName("JPEG 시그니처지만 구조가 깨져 있으면")
        class ContextWithBrokenJpeg {

            @Test
            @DisplayName("FILE415-EXT_UNSUPPORTED로 거절한다")
            void itRejectsBrokenJpeg() {
                // given
                byte[] brokenJpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x7F, (byte) 0xFF, 'J', 'F'};
                MockMultipartFile file = new MockMultipartFile("image", "broken.jpg", "image/jpeg", brokenJpeg);

                // when & then
                assertThatThrownBy(() -> s3Service.upload(List.of(file)))
                        .isInstanceOf(GeneralException.class)
                        .extracting(exception -> ((GeneralException) exception).getCode())
                        .isEqualTo(ErrorStatus._INVALID_FILE_EXTENSION);
            }
        }
    }

    @Nested
    @DisplayName("원본과 썸네일을 함께 업로드할 때")
    class DescribeUploadWithThumbnail {

        @Test
        @DisplayName("원본 파일명 없이 표준 Content-Type으로 원본과 썸네일을 저장한다")
        void itStoresOriginalAndThumbnail() {
            // given
            MockMultipartFile file = new MockMultipartFile("image", "home-photo.jpeg", "image/jpeg", jpegWithGps);

            // when
            UploadedImage uploaded =
                    s3Service.uploadWithThumbnail(List.of(file)).get(0);

            // then
            String originalUrl = uploaded.originalUrl();
            String thumbnailUrl = uploaded.thumbnailUrl();
            String originalKey = originalUrl.substring(originalUrl.indexOf("/" + BUCKET + "/") + BUCKET.length() + 2);
            String thumbnailKey =
                    thumbnailUrl.substring(thumbnailUrl.indexOf("/" + BUCKET + "/") + BUCKET.length() + 2);
            assertThat(originalKey).matches("[0-9a-f-]{36}\\.jpg");
            assertThat(thumbnailKey).matches("thumb_[0-9a-f-]{36}\\.jpg");
            assertThat(s3Client.headObject(request -> request.bucket(BUCKET).key(thumbnailKey))
                            .contentType())
                    .isEqualTo("image/jpeg");
        }
    }
}
