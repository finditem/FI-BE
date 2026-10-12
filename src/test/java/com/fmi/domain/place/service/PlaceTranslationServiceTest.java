package com.fmi.domain.place.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fmi.domain.Enum.LanguageCode;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.security.jwt.JwtTokenProvider;
import com.fmi.domain.place.data.*;
import com.fmi.domain.place.data.enums.*;
import com.fmi.domain.place.repository.*;
import com.fmi.domain.place.web.dto.response.PlaceManagementResponse;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.external.storage.S3Service;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.dto.UploadedImage;
import com.fmi.support.IntegrationTestSupport;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("PlaceTranslationService")
class PlaceTranslationServiceTest extends IntegrationTestSupport {
    @Autowired
    PlaceService placeService;

    @Autowired
    PlaceFavoriteService favoriteService;

    @Autowired
    PlaceTranslationRepository translations;

    @Autowired
    PlaceRepository places;

    @Autowired
    PlaceFavoriteRepository favorites;

    @Autowired
    UserRepository users;

    @Autowired
    S3Service s3;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    TestRestTemplate http;

    @Autowired
    JwtTokenProvider tokens;

    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        translations.deleteAll();
        favorites.deleteAll();
        places.deleteAll();
        reset(s3);
        when(s3.uploadWithThumbnail(anyList()))
                .thenReturn(
                        List.of(new UploadedImage("https://storage.test/original", "https://storage.test/thumbnail")));
    }

    @AfterEach
    void tearDown() {
        translations.deleteAll();
        favorites.deleteAll();
        places.deleteAll();
    }

    @Nested
    @DisplayName("장소 영문 정보를 저장하고 조회할 때")
    class DescribeTranslations {
        @Test
        @DisplayName("네 조회 경로가 같은 영문과 항목별 한국어 대체를 반환하고 커서를 유지한다")
        void itUsesSamePartialTranslationAcrossAllQueries() throws Exception {
            // given
            List<PlaceDailySchedule> schedules = Arrays.stream(DayOfWeek.values())
                    .map(day -> new PlaceDailySchedule(
                            day,
                            false,
                            List.of(new PlaceTimeRange(
                                    PlaceBusinessHourType.BUSINESS, LocalTime.of(10, 0), LocalTime.of(22, 0)))))
                    .toList();
            MockMultipartFile thumbnail =
                    new MockMultipartFile("thumbnail", "thumbnail.png", "image/png", new byte[] {1});
            PlaceUpsertCommand command = new PlaceUpsertCommand(
                    "카페",
                    "한국어 주소",
                    37.5421,
                    127.0549,
                    "성수역",
                    100,
                    PlaceType.CAFE,
                    null,
                    null,
                    schedules,
                    "  Cafe  ",
                    "   ",
                    "Seongsu Station");
            Long id = placeService.create(command, thumbnail);
            User user = users.save(User.builder()
                    .nickname("번역 조회")
                    .email("place-language@test.com")
                    .password("password")
                    .role(Role.USER)
                    .preferredLanguage(LanguageCode.KO)
                    .build());
            favoriteService.save(id, user.getEmail());
            Long otherId = placeService.create(command, thumbnail);
            favoriteService.save(otherId, user.getEmail());
            // when
            PlaceSummary english = placeService.getPlaceSummary(id, user.getEmail(), LanguageCode.EN);
            PlaceSummary korean = placeService.getPlaceSummary(id, user.getEmail());
            List<PlaceSummary> home = placeService.getHomePlaces(null, user.getId(), LanguageCode.EN);
            PlaceMapSearchResult map =
                    placeService.getMapPlaces(37.5421, 127.0549, 6, PlaceType.CAFE, user.getEmail(), LanguageCode.EN);
            FavoritePlacePage englishFavorites =
                    favoriteService.getFavorites(user.getEmail(), null, 1, LanguageCode.EN);
            FavoritePlacePage koreanFavorites = favoriteService.getFavorites(user.getEmail(), null, 1);
            // then
            assertThat(english.name()).isEqualTo("Cafe");
            assertThat(english.address()).isEqualTo("한국어 주소");
            assertThat(english.station()).isEqualTo("Seongsu Station");
            assertThat(english.favorite()).isTrue();
            assertThat(english)
                    .usingRecursiveComparison()
                    .ignoringFields("name", "station")
                    .isEqualTo(korean);
            assertThat(home).extracting(PlaceSummary::placeId).containsExactly(otherId, id);
            assertThat(home).allSatisfy(summary -> {
                assertThat(summary.name()).isEqualTo("Cafe");
                assertThat(summary.address()).isEqualTo("한국어 주소");
            });
            assertThat(map.places()).extracting(PlaceSummary::placeId).containsExactly(otherId, id);
            assertThat(map.places())
                    .allSatisfy(summary -> assertThat(summary.station()).isEqualTo("Seongsu Station"));
            assertThat(englishFavorites)
                    .usingRecursiveComparison()
                    .ignoringFields("places")
                    .isEqualTo(koreanFavorites);
            assertThat(englishFavorites.places())
                    .extracting(PlaceSummary::placeId)
                    .containsExactlyElementsOf(koreanFavorites.places().stream()
                            .map(PlaceSummary::placeId)
                            .toList());
            assertThat(englishFavorites.places())
                    .allSatisfy(summary -> assertThat(summary.name()).isEqualTo("Cafe"));
            assertThat(translations.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("수정 시 영문을 전체 대체하고 미등록 필드는 관리자 JSON에도 null로 포함한다")
        void itReplacesAndRemovesAdministratorTranslations() throws Exception {
            // given
            List<PlaceDailySchedule> schedules = Arrays.stream(DayOfWeek.values())
                    .map(day -> new PlaceDailySchedule(
                            day,
                            false,
                            List.of(new PlaceTimeRange(
                                    PlaceBusinessHourType.BUSINESS, LocalTime.of(10, 0), LocalTime.of(22, 0)))))
                    .toList();
            MockMultipartFile thumbnail =
                    new MockMultipartFile("thumbnail", "thumbnail.png", "image/png", new byte[] {1});
            Long id = placeService.create(
                    new PlaceUpsertCommand(
                            "카페",
                            "주소",
                            37.5421,
                            127.0549,
                            "성수역",
                            100,
                            PlaceType.CAFE,
                            null,
                            null,
                            schedules,
                            "Cafe",
                            "Address",
                            "Station"),
                    thumbnail);
            // when
            placeService.update(
                    id,
                    new PlaceUpsertCommand(
                            "새 카페",
                            "주소",
                            37.5421,
                            127.0549,
                            "성수역",
                            100,
                            PlaceType.CAFE,
                            null,
                            null,
                            schedules,
                            null,
                            "New Address",
                            null),
                    null);
            PlaceManagementDetail partial = placeService.getManagementDetail(id);
            placeService.update(
                    id,
                    new PlaceUpsertCommand(
                            "새 카페", "주소", 37.5421, 127.0549, "성수역", 100, PlaceType.CAFE, null, null, schedules),
                    null);
            // then
            assertThat(partial.nameEn()).isNull();
            assertThat(partial.addressEn()).isEqualTo("New Address");
            assertThat(partial.stationEn()).isNull();
            assertThat(translations.count()).isZero();
            assertThat(placeService.getPlaceSummary(id, null, LanguageCode.EN).name())
                    .isEqualTo("새 카페");
            var json = mapper.valueToTree(PlaceManagementResponse.from(placeService.getManagementDetail(id)));
            assertThat(json.has("nameEn")).isTrue();
            assertThat(json.get("nameEn").isNull()).isTrue();
            assertThat(json.get("addressEn").isNull()).isTrue();
            assertThat(json.get("stationEn").isNull()).isTrue();
        }

        @Test
        @DisplayName("영문 최대 길이는 허용하고 초과 입력은 S3 호출과 DB 변경 전에 거절한다")
        void itValidatesLengthsBeforeExternalEffects() throws Exception {
            // given
            List<PlaceDailySchedule> schedules = Arrays.stream(DayOfWeek.values())
                    .map(day -> new PlaceDailySchedule(
                            day,
                            false,
                            List.of(new PlaceTimeRange(
                                    PlaceBusinessHourType.BUSINESS, LocalTime.of(10, 0), LocalTime.of(22, 0)))))
                    .toList();
            MockMultipartFile thumbnail =
                    new MockMultipartFile("thumbnail", "thumbnail.png", "image/png", new byte[] {1});
            PlaceUpsertCommand valid = new PlaceUpsertCommand(
                    "카페",
                    "주소",
                    37.5421,
                    127.0549,
                    "성수역",
                    100,
                    PlaceType.CAFE,
                    null,
                    null,
                    schedules,
                    "n".repeat(100),
                    "a".repeat(510),
                    "s".repeat(200));
            Long id = placeService.create(valid, thumbnail);
            clearInvocations(s3);
            // when & then
            for (int field = 0; field < 3; field++) {
                PlaceUpsertCommand invalid = new PlaceUpsertCommand(
                        "변경",
                        "주소",
                        37.5421,
                        127.0549,
                        "성수역",
                        100,
                        PlaceType.CAFE,
                        null,
                        null,
                        schedules,
                        "n".repeat(field == 0 ? 101 : 100),
                        "a".repeat(field == 1 ? 511 : 510),
                        "s".repeat(field == 2 ? 201 : 200));
                assertThatThrownBy(() -> placeService.create(invalid, thumbnail))
                        .isInstanceOf(GeneralException.class);
                assertThatThrownBy(() -> placeService.update(id, invalid, thumbnail))
                        .isInstanceOf(GeneralException.class);
            }
            verifyNoInteractions(s3);
            assertThat(places.count()).isEqualTo(1);
            assertThat(placeService.getManagementDetail(id).nameEn()).hasSize(100);
            assertThat(placeService.getManagementDetail(id).addressEn()).hasSize(510);
            assertThat(placeService.getManagementDetail(id).stationEn()).hasSize(200);
        }

        @Test
        @DisplayName("동일 transaction이 실패하면 장소와 번역을 함께 롤백한다")
        void itRollsBackTranslationsAndOriginalTogether() throws Exception {
            // given
            List<PlaceDailySchedule> schedules = Arrays.stream(DayOfWeek.values())
                    .map(day -> new PlaceDailySchedule(
                            day,
                            false,
                            List.of(new PlaceTimeRange(
                                    PlaceBusinessHourType.BUSINESS, LocalTime.of(10, 0), LocalTime.of(22, 0)))))
                    .toList();
            MockMultipartFile thumbnail =
                    new MockMultipartFile("thumbnail", "thumbnail.png", "image/png", new byte[] {1});
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            // when
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                        Long id = placeService.create(
                                new PlaceUpsertCommand(
                                        "카페",
                                        "주소",
                                        37.5421,
                                        127.0549,
                                        "성수역",
                                        100,
                                        PlaceType.CAFE,
                                        null,
                                        null,
                                        schedules,
                                        "Cafe",
                                        null,
                                        null),
                                thumbnail);
                        assertThat(translations.findByPlace_IdAndLanguageCode(id, LanguageCode.EN))
                                .isPresent();
                        throw new IllegalStateException("rollback verification");
                    }))
                    .isInstanceOf(IllegalStateException.class);
            // then
            assertThat(places.count()).isZero();
            assertThat(translations.count()).isZero();
        }

        @Test
        @DisplayName("실행 서버에서 요청 언어와 기본값 및 잘못된 언어의 오류 응답을 확인한다")
        void itServesLanguageContractThroughRunningHttpServer() throws Exception {
            // given
            List<PlaceDailySchedule> schedules = Arrays.stream(DayOfWeek.values())
                    .map(day -> new PlaceDailySchedule(
                            day,
                            false,
                            List.of(new PlaceTimeRange(
                                    PlaceBusinessHourType.BUSINESS, LocalTime.of(10, 0), LocalTime.of(22, 0)))))
                    .toList();
            MockMultipartFile thumbnail =
                    new MockMultipartFile("thumbnail", "thumbnail.png", "image/png", new byte[] {1});
            Long id = placeService.create(
                    new PlaceUpsertCommand(
                            "카페",
                            "주소",
                            37.5421,
                            127.0549,
                            "성수역",
                            100,
                            PlaceType.CAFE,
                            null,
                            null,
                            schedules,
                            "Cafe",
                            null,
                            null),
                    thumbnail);
            // when & then
            for (String path : List.of(
                    "/places",
                    "/main/places/" + id + "/summary",
                    "/main/places/search-location?latitude=37.5421&longitude=127.0549&type=CAFE")) {
                String separator = path.contains("?") ? "&" : "?";
                var english = http.getForEntity(path + separator + "language=EN", String.class);
                assertThat(english.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(english.getBody()).contains("Cafe");
                assertThat(http.getForObject(path, String.class)).contains("카페");
                for (String invalid : List.of("en", "", "FR")) {
                    var error = http.getForEntity(path + separator + "language=" + invalid, String.class);
                    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(error.getBody()).contains("PLACE400-INVALID_REQUEST");
                }
            }
            User viewer = users.save(User.builder()
                    .nickname("영문 선호 관리자")
                    .email("place-http-admin@test.com")
                    .password("password")
                    .role(Role.ADMIN)
                    .preferredLanguage(LanguageCode.EN)
                    .build());
            favoriteService.save(id, viewer.getEmail());
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tokens.createAccessToken(viewer.getEmail(), Map.of()));
            HttpEntity<Void> authenticated = new HttpEntity<>(headers);
            for (String path : List.of("/places", "/main/places/" + id + "/summary", "/users/me/favorite-places")) {
                var defaultResponse = http.exchange(path, HttpMethod.GET, authenticated, String.class);
                assertThat(defaultResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(defaultResponse.getBody()).contains("카페");
                assertThat(http.exchange(path + "?language=EN", HttpMethod.GET, authenticated, String.class)
                                .getBody())
                        .contains("Cafe");
                for (String invalid : List.of("en", "", "FR")) {
                    var error =
                            http.exchange(path + "?language=" + invalid, HttpMethod.GET, authenticated, String.class);
                    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(error.getBody()).contains("PLACE400-INVALID_REQUEST");
                }
            }
            var administrator = http.exchange("/admin/places/" + id, HttpMethod.GET, authenticated, String.class);
            assertThat(administrator.getStatusCode()).isEqualTo(HttpStatus.OK);
            var original = mapper.readTree(administrator.getBody()).path("result");
            assertThat(original.path("name").asText()).isEqualTo("카페");
            assertThat(original.path("nameEn").asText()).isEqualTo("Cafe");
            assertThat(original.has("addressEn")).isTrue();
            assertThat(original.path("addressEn").isNull()).isTrue();
        }

        @Test
        @DisplayName("영문 최초 등록이 경합해도 성공한 수정의 한국어와 영문을 함께 저장한다")
        void itCommitsOriginalAndTranslationTogetherUnderConcurrentUpdates() throws Exception {
            // given
            List<PlaceDailySchedule> schedules = Arrays.stream(DayOfWeek.values())
                    .map(day -> new PlaceDailySchedule(
                            day,
                            false,
                            List.of(new PlaceTimeRange(
                                    PlaceBusinessHourType.BUSINESS, LocalTime.of(10, 0), LocalTime.of(22, 0)))))
                    .toList();
            MockMultipartFile thumbnail =
                    new MockMultipartFile("thumbnail", "thumbnail.png", "image/png", new byte[] {1});
            Long id = placeService.create(
                    new PlaceUpsertCommand(
                            "카페", "주소", 37.5421, 127.0549, "성수역", 100, PlaceType.CAFE, null, null, schedules),
                    thumbnail);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            try {
                for (String name : List.of("First", "Second")) {
                    results.add(executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("start timeout");
                        }
                        try {
                            placeService.update(
                                    id,
                                    new PlaceUpsertCommand(
                                            name,
                                            "주소",
                                            37.5421,
                                            127.0549,
                                            "성수역",
                                            100,
                                            PlaceType.CAFE,
                                            null,
                                            null,
                                            schedules,
                                            name,
                                            null,
                                            null),
                                    null);
                            return true;
                        } catch (DataAccessException exception) {
                            return false;
                        }
                    }));
                }
                // when
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                int successes = 0;
                for (Future<Boolean> result : results) {
                    if (result.get(30, TimeUnit.SECONDS)) {
                        successes++;
                    }
                }
                // then
                assertThat(successes).isGreaterThanOrEqualTo(1);
                PlaceManagementDetail detail = placeService.getManagementDetail(id);
                assertThat(detail.name()).isIn("First", "Second");
                assertThat(detail.nameEn()).isEqualTo(detail.name());
                assertThat(translations.count()).isEqualTo(1);
            } finally {
                start.countDown();
                executor.shutdownNow();
            }
        }
    }
}
