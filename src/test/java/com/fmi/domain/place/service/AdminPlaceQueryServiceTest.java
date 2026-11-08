package com.fmi.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.place.service.result.AdminPlaceListResult;
import com.fmi.domain.place.service.result.AdminPlaceResult;
import com.fmi.domain.place.web.dto.request.AdminPlaceListRequest;
import com.fmi.domain.place.web.dto.response.AdminPlacePageResponse;
import com.fmi.domain.place.web.dto.response.DeletedPlacePageResponse;
import com.fmi.global.data.EntityStatus;
import com.fmi.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("AdminPlaceQueryService")
@Transactional
class AdminPlaceQueryServiceTest extends IntegrationTestSupport {
    @Autowired
    private AdminPlaceQueryService service;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM place_favorite");
        jdbc.update("DELETE FROM place_translation");
        jdbc.update("DELETE FROM place_business_hour");
        jdbc.update("DELETE FROM place_operation_period");
        jdbc.update("DELETE FROM place");
    }

    @Nested
    @DisplayName("관리자 장소 목록을 조회할 때")
    class DescribeGetPlaces {
        @Test
        @DisplayName("등록 시각이 같은 23개 장소를 누락과 중복 없이 두 페이지로 반환한다")
        void itPaginatesEqualCreationTimesById() {
            // given
            LocalDateTime time = LocalDateTime.of(2026, 10, 1, 10, 0, 0, 123456000);
            List<Long> ids = new ArrayList<>();
            for (int index = 0; index < 23; index++) {
                jdbc.update("""
                    INSERT INTO place (name,address,latitude,longitude,station,station_distance_meters,
                    type,thumbnail_url,entity_status,created_at,updated_at)
                    VALUES (?, '서울',37.5,127.0,'성수역',100,'CAFE','thumbnail','ACTIVE',?,?)
                    """, "카페" + index, Timestamp.valueOf(time), Timestamp.valueOf(time));
                ids.add(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
            }
            // when
            AdminPlaceListResult first =
                    service.getPlaces(new AdminPlaceListRequest(null, null, null, null), EntityStatus.ACTIVE);
            AdminPlacePageResponse response = AdminPlacePageResponse.from(first);
            AdminPlaceListResult second = service.getPlaces(
                    new AdminPlaceListRequest(
                            null,
                            null,
                            response.nextCursor().time().toString(),
                            response.nextCursor().placeId().toString()),
                    EntityStatus.ACTIVE);
            // then
            assertThat(first.places()).hasSize(20);
            assertThat(first.hasNext()).isTrue();
            assertThat(first.places().get(0).placeId()).isEqualTo(ids.get(22));
            assertThat(second.places())
                    .extracting(AdminPlaceResult::placeId)
                    .containsExactly(ids.get(2), ids.get(1), ids.get(0));
            assertThat(second.hasNext()).isFalse();
            assertThat(AdminPlacePageResponse.from(second).nextCursor()).isNull();
            List<Long> actual = new ArrayList<>();
            first.places().forEach(place -> actual.add(place.placeId()));
            second.places().forEach(place -> actual.add(place.placeId()));
            assertThat(actual).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(ids);
        }

        @Test
        @DisplayName("종료된 팝업을 포함하고 삭제된 장소를 제외한다")
        void itIncludesEndedPopupsAndExcludesDeletedPlaces() {
            // given
            jdbc.update("""
                INSERT INTO place (id,name,address,latitude,longitude,station,station_distance_meters,type,
                thumbnail_url,entity_status,created_at,updated_at,deleted_at) VALUES
                (8001,'종료 팝업','서울',37.5,127.0,'성수역',100,'POPUP','thumbnail','ACTIVE','2026-01-01','2026-01-01',NULL),
                (8002,'삭제 카페','서울',37.5,127.0,'성수역',100,'CAFE','thumbnail','DELETED','2026-01-02','2026-01-02','2026-01-03')
                """);
            jdbc.update("""
                INSERT INTO place_operation_period (place_id,start_date,end_date,closing_at,entity_status,created_at,updated_at)
                VALUES (8001,'2026-01-01','2026-01-02','2026-01-02 22:00:00','ACTIVE','2026-01-01','2026-01-01')
                """);
            // when
            AdminPlaceListResult result =
                    service.getPlaces(new AdminPlaceListRequest("POPUP", null, null, null), EntityStatus.ACTIVE);
            // then
            assertThat(result.places()).extracting(AdminPlaceResult::placeId).containsExactly(8001L);
        }

        @Test
        @DisplayName("한국어와 영문 이름을 부분 검색하고 특수문자는 그대로 검색한다")
        void itSearchesBothLanguagesAndEscapesWildcards() {
            // given
            jdbc.update("""
                INSERT INTO place (id,name,address,latitude,longitude,station,station_distance_meters,type,
                thumbnail_url,entity_status,created_at,updated_at) VALUES
                (8101,'성수 100%_!','서울',37.5,127.0,'성수역',100,'CAFE','thumbnail','ACTIVE','2026-01-01','2026-01-01'),
                (8102,'성수 100abc','서울',37.5,127.0,'성수역',100,'RESTAURANT','thumbnail','ACTIVE','2026-01-02','2026-01-02')
                """);
            jdbc.update("""
                INSERT INTO place_translation (place_id,language_code,translated_name,created_at,updated_at)
                VALUES (8101,'EN','Seongsu Cafe','2026-01-01','2026-01-01')
                """);
            // when
            AdminPlaceListResult english =
                    service.getPlaces(new AdminPlaceListRequest(null, "  CAFE  ", null, null), EntityStatus.ACTIVE);
            AdminPlaceListResult korean =
                    service.getPlaces(new AdminPlaceListRequest("CAFE", "성수", null, null), EntityStatus.ACTIVE);
            AdminPlaceListResult literal =
                    service.getPlaces(new AdminPlaceListRequest(null, "%_!", null, null), EntityStatus.ACTIVE);
            AdminPlaceListResult missing =
                    service.getPlaces(new AdminPlaceListRequest(null, "없는 이름", null, null), EntityStatus.ACTIVE);
            AdminPlaceListResult blank =
                    service.getPlaces(new AdminPlaceListRequest(null, "  ", null, null), EntityStatus.ACTIVE);
            // then
            assertThat(english.places()).extracting(AdminPlaceResult::placeId).containsExactly(8101L);
            assertThat(english.places().get(0).nameEn()).isEqualTo("Seongsu Cafe");
            assertThat(korean.places()).extracting(AdminPlaceResult::placeId).containsExactly(8101L);
            assertThat(literal.places()).extracting(AdminPlaceResult::placeId).containsExactly(8101L);
            assertThat(missing.places()).isEmpty();
            assertThat(missing.hasNext()).isFalse();
            assertThat(blank.places()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("삭제된 장소 목록을 조회할 때")
    class DescribeGetDeletedPlaces {
        @Test
        @DisplayName("삭제 시각과 ID 순서로 검색하며 커서 대상 복구 후에도 다음 페이지를 조회한다")
        void itContinuesAfterRestoringCursorPlace() {
            // given
            for (int index = 0; index < 22; index++) {
                jdbc.update("""
                    INSERT INTO place (name,address,latitude,longitude,station,station_distance_meters,type,
                    thumbnail_url,entity_status,created_at,updated_at,deleted_at)
                    VALUES ('삭제 카페','서울',37.5,127.0,'성수역',100,'CAFE','thumbnail','DELETED',
                    '2026-01-01','2026-01-01','2026-02-01')
                    """);
            }
            entityManager
                    .createQuery("update Place p set p.deletedAt = :deletedAt")
                    .setParameter("deletedAt", LocalDateTime.of(2026, 2, 1, 0, 0))
                    .executeUpdate();
            Long earliestId = jdbc.queryForObject("SELECT MIN(id) FROM place", Long.class);
            entityManager
                    .createQuery("update Place p set p.deletedAt = :deletedAt where p.id = :placeId")
                    .setParameter("deletedAt", LocalDateTime.of(2026, 3, 1, 0, 0))
                    .setParameter("placeId", earliestId)
                    .executeUpdate();
            AdminPlaceListResult first =
                    service.getPlaces(new AdminPlaceListRequest("CAFE", "삭제", null, null), EntityStatus.DELETED);
            DeletedPlacePageResponse response = DeletedPlacePageResponse.from(first);
            Long restoredId = first.places().get(19).placeId();
            placeService.restore(restoredId);
            entityManager.flush();
            // when
            AdminPlaceListResult second = service.getPlaces(
                    new AdminPlaceListRequest(
                            "CAFE",
                            "삭제",
                            response.nextCursor().time().toString(),
                            response.nextCursor().placeId().toString()),
                    EntityStatus.DELETED);
            AdminPlaceListResult active =
                    service.getPlaces(new AdminPlaceListRequest("CAFE", "삭제", null, null), EntityStatus.ACTIVE);
            // then
            assertThat(first.hasNext()).isTrue();
            assertThat(first.places().get(0).deletedAt()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
            assertThat(second.places())
                    .hasSize(2)
                    .extracting(AdminPlaceResult::placeId)
                    .doesNotContain(restoredId);
            assertThat(second.hasNext()).isFalse();
            assertThat(DeletedPlacePageResponse.from(second).nextCursor()).isNull();
            assertThat(active.places()).extracting(AdminPlaceResult::placeId).containsExactly(restoredId);
        }
    }
}
