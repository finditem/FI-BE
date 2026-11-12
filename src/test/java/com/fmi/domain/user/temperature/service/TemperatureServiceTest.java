package com.fmi.domain.user.temperature.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.domain.auth.data.enums.WithdrawalReason;
import com.fmi.domain.user.temperature.data.TemperatureActivity;
import com.fmi.domain.user.temperature.data.UserTemperature;
import com.fmi.domain.user.temperature.data.enums.TemperatureActivityType;
import com.fmi.domain.user.temperature.repository.TemperatureActivityRepository;
import com.fmi.domain.user.temperature.repository.UserTemperatureRepository;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Transactional
@DisplayName("TemperatureService")
class TemperatureServiceTest extends IntegrationTestSupport {

    // IntegrationTestSupport의 고정 시각 2026-09-10T12:00 (Asia/Seoul)
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 12, 0);
    private static final LocalDateTime PERIOD_START = NOW.minusYears(1);

    private static final AtomicLong SOURCE_ID = new AtomicLong(970000L);

    @Autowired
    private TemperatureService temperatureService;

    @Autowired
    private TemperatureActivityRepository temperatureActivityRepository;

    @Autowired
    private UserTemperatureRepository userTemperatureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("활동을 기록할 때")
    class DescribeRecord {

        @Test
        @DisplayName("활동 유형의 가중치로 원장에 저장한다")
        void itSavesActivityWithTypeDelta() {
            // given
            User user = saveUser("record-user@test.com");
            LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 1, 10, 0);

            // when
            temperatureService.record(TemperatureActivityType.RETURN_COMPLETED, user.getId(), 970001L, occurredAt);

            // then
            assertThat(temperatureActivityRepository.findAll().stream()
                            .filter(activity -> activity.getUser().getId().equals(user.getId())))
                    .singleElement()
                    .satisfies(activity -> {
                        assertThat(activity.getActivityType()).isEqualTo(TemperatureActivityType.RETURN_COMPLETED);
                        assertThat(activity.getSourceId()).isEqualTo(970001L);
                        assertThat(activity.getDeltaTenths()).isEqualTo(20);
                        assertThat(activity.getOccurredAt()).isEqualTo(occurredAt);
                    });
        }

        @Test
        @DisplayName("대상 사용자가 없으면 기록하지 않는다")
        void itSkipsWithoutUser() {
            // given
            long before = temperatureActivityRepository.count();

            // when
            temperatureService.record(TemperatureActivityType.REVIEW_RECEIVED, null, 970002L, NOW);

            // then
            assertThat(temperatureActivityRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("같은 활동 원본을 다시 기록하면 unique 제약으로 막는다")
        void itRejectsDuplicateSource() {
            // given
            User user = saveUser("duplicate-user@test.com");
            temperatureService.record(TemperatureActivityType.FOUND_POST, user.getId(), 970003L, NOW);
            temperatureActivityRepository.flush();

            // when & then
            assertThatThrownBy(() -> {
                        temperatureService.record(TemperatureActivityType.FOUND_POST, user.getId(), 970003L, NOW);
                        temperatureActivityRepository.flush();
                    })
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("온도를 다시 계산할 때")
    class DescribeRecalculate {

        @Test
        @DisplayName("최근 1년 활동을 36.5에 더한다")
        void itAddsRecentActivities() {
            // given 습득 게시 6건, 반환 완료 9건, 따뜻한 후기 15건
            User user = saveUser("example-user@test.com");
            saveActivities(user, TemperatureActivityType.FOUND_POST, 6, NOW.minusDays(1));
            saveActivities(user, TemperatureActivityType.RETURN_COMPLETED, 9, NOW.minusDays(1));
            saveActivities(user, TemperatureActivityType.REVIEW_RECEIVED, 15, NOW.minusDays(1));

            // when
            temperatureService.recalculate();

            // then 36.5 + 3.0 + 18.0 + 15.0
            UserTemperature result =
                    userTemperatureRepository.findById(user.getId()).orElseThrow();
            assertThat(result.getTemperatureTenths()).isEqualTo(725);
            assertThat(result.getCalculatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("1년 전 시각에 발생한 활동은 포함하고 그 이전 활동은 제외한다")
        void itAppliesOneYearBoundary() {
            // given
            User user = saveUser("boundary-user@test.com");
            saveActivities(user, TemperatureActivityType.RETURN_COMPLETED, 1, PERIOD_START);
            saveActivities(user, TemperatureActivityType.REVIEW_RECEIVED, 1, PERIOD_START.minusNanos(1000));

            // when
            temperatureService.recalculate();

            // then
            assertThat(userTemperatureRepository
                            .findById(user.getId())
                            .orElseThrow()
                            .getTemperatureTenths())
                    .isEqualTo(385);
        }

        @Test
        @DisplayName("활동이 모두 산정 기간을 벗어나면 36.5로 돌아간다")
        void itResetsWhenAllActivitiesExpired() {
            // given
            User user = saveUser("expired-user@test.com");
            saveActivities(user, TemperatureActivityType.RETURN_COMPLETED, 3, NOW.minusDays(1));
            temperatureService.recalculate();
            temperatureActivityRepository.deleteAll(temperatureActivityRepository.findAll().stream()
                    .filter(activity -> activity.getUser().getId().equals(user.getId()))
                    .toList());
            saveActivities(user, TemperatureActivityType.RETURN_COMPLETED, 3, PERIOD_START.minusDays(1));

            // when
            temperatureService.recalculate();

            // then
            assertThat(userTemperatureRepository
                            .findById(user.getId())
                            .orElseThrow()
                            .getTemperatureTenths())
                    .isEqualTo(365);
        }

        @Test
        @DisplayName("99℃를 넘으면 99℃로 저장한다")
        void itClampsToMaximum() {
            // given 36.5 + 2.0 × 32 = 100.5
            User user = saveUser("max-user@test.com");
            saveActivities(user, TemperatureActivityType.RETURN_COMPLETED, 32, NOW.minusDays(1));

            // when
            temperatureService.recalculate();

            // then
            assertThat(userTemperatureRepository
                            .findById(user.getId())
                            .orElseThrow()
                            .getTemperatureTenths())
                    .isEqualTo(990);
        }
    }

    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("사용자와 온도 데이터의 생명주기를 처리할 때")
    class DescribeUserRelationship {

        @Test
        @DisplayName("커밋한 원장과 SQL 집계를 새 트랜잭션에서 조회하면 사용자 참조와 공유 기본키가 일치한다")
        void itReloadsCommittedActivityAndSharedPrimaryKeyWithLazyUser() {
            // given
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            Long userId =
                    tx.execute(status -> saveUser("temperature-lazy@test.com").getId());
            long sourceId = SOURCE_ID.incrementAndGet();
            try {
                // when
                tx.executeWithoutResult(status -> {
                    temperatureService.record(TemperatureActivityType.FOUND_POST, userId, sourceId, NOW);
                    temperatureService.recalculate();
                });
                Long activityId = jdbcTemplate.queryForObject(
                        "select id from temperature_activity where user_id=? and source_id=?",
                        Long.class,
                        userId,
                        sourceId);

                // then: 각 관계를 별도 영속성 컨텍스트에서 조회한다.
                tx.executeWithoutResult(status -> {
                    TemperatureActivity activity =
                            temperatureActivityRepository.findById(activityId).orElseThrow();
                    assertThat(Hibernate.isInitialized(activity.getUser())).isFalse();
                    assertThat(activity.getUser().getId()).isEqualTo(userId);
                    assertThat(Hibernate.isInitialized(activity.getUser())).isFalse();
                    assertThat(activity.getUser().getEmail()).isEqualTo("temperature-lazy@test.com");
                    assertThat(Hibernate.isInitialized(activity.getUser())).isTrue();
                });
                tx.executeWithoutResult(status -> {
                    UserTemperature temperature =
                            userTemperatureRepository.findById(userId).orElseThrow();
                    assertThat(temperature.getUserId()).isEqualTo(userId);
                    assertThat(temperature.getTemperatureTenths()).isEqualTo(370);
                    assertThat(Hibernate.isInitialized(temperature.getUser())).isFalse();
                    assertThat(temperature.getUser().getId()).isEqualTo(userId);
                    assertThat(Hibernate.isInitialized(temperature.getUser())).isFalse();
                    assertThat(temperature.getUser().getEmail()).isEqualTo("temperature-lazy@test.com");
                });
            } finally {
                tx.executeWithoutResult(status -> userRepository.deleteById(userId));
            }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 프록시로 원장을 저장하면 FK 오류로 롤백되고 사용자를 생성하지 않는다")
        void itRejectsActivityForMissingUser() {
            // given
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            long missingUserId = Long.MAX_VALUE;
            long sourceId = SOURCE_ID.incrementAndGet();
            assertThat(userRepository.existsById(missingUserId)).isFalse();

            // when & then
            assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                        temperatureService.record(TemperatureActivityType.FOUND_POST, missingUserId, sourceId, NOW);
                        temperatureActivityRepository.flush();
                    }))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .rootCause()
                    .isInstanceOfSatisfying(SQLIntegrityConstraintViolationException.class, error -> {
                        assertThat(error.getErrorCode()).isEqualTo(1452);
                        assertThat(error.getSQLState()).isEqualTo("23000");
                        assertThat(error.getMessage()).contains("fk_temperature_activity_user");
                    });
            assertThat(userRepository.existsById(missingUserId)).isFalse();
            assertThat(jdbcTemplate.queryForObject(
                            "select count(*) from temperature_activity where source_id=?", Integer.class, sourceId))
                    .isZero();
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 집계를 저장하면 FK 오류로 롤백된다")
        void itRejectsSnapshotForMissingUser() {
            // given
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            long missingUserId = Long.MAX_VALUE;
            assertThat(userRepository.existsById(missingUserId)).isFalse();

            // when & then: 집계의 실제 쓰기 경로와 같은 SQL INSERT로 FK를 확인한다.
            assertThatThrownBy(() -> tx.executeWithoutResult(status -> jdbcTemplate.update(
                            "insert into user_temperature (user_id,temperature_tenths,calculated_at) values (?,?,?)",
                            missingUserId,
                            365,
                            NOW)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .rootCause()
                    .isInstanceOfSatisfying(SQLIntegrityConstraintViolationException.class, error -> {
                        assertThat(error.getErrorCode()).isEqualTo(1452);
                        assertThat(error.getSQLState()).isEqualTo("23000");
                        assertThat(error.getMessage()).contains("fk_user_temperature_user");
                    });
            assertThat(userTemperatureRepository.existsById(missingUserId)).isFalse();
            assertThat(userRepository.existsById(missingUserId)).isFalse();
        }

        @Test
        @DisplayName("사용자를 소프트 삭제하면 사용자 행과 온도 원장 및 집계를 보존한다")
        void itKeepsTemperatureWhenUserIsSoftDeleted() {
            // given
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            Long userId = tx.execute(
                    status -> saveUser("temperature-withdraw@test.com").getId());
            long sourceId = SOURCE_ID.incrementAndGet();
            try {
                tx.executeWithoutResult(status -> {
                    temperatureService.record(TemperatureActivityType.FOUND_POST, userId, sourceId, NOW);
                    temperatureService.recalculate();
                });

                // when: 탈퇴에서 사용하는 도메인 메서드를 실행한다.
                tx.executeWithoutResult(status -> userRepository
                        .findById(userId)
                        .orElseThrow()
                        .withdraw(List.of(WithdrawalReason.NOT_USING), null, NOW));

                // then
                tx.executeWithoutResult(status -> {
                    assertThat(userRepository.findById(userId).orElseThrow().getDeletedAt())
                            .isEqualTo(NOW);
                    assertThat(jdbcTemplate.queryForObject(
                                    "select count(*) from temperature_activity where user_id=?", Integer.class, userId))
                            .isEqualTo(1);
                    UserTemperature temperature =
                            userTemperatureRepository.findById(userId).orElseThrow();
                    assertThat(temperature.getTemperatureTenths()).isEqualTo(370);
                    assertThat(temperature.getUser().getId()).isEqualTo(userId);
                });
            } finally {
                tx.executeWithoutResult(status -> userRepository.deleteById(userId));
            }
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("사용자 실제 삭제를 커밋하면 온도만 cascade 삭제하고 롤백하면 모두 복원하며 다른 사용자는 보존한다")
        void itCascadesPhysicalDeletionAndHonorsRollback(boolean rollback) {
            // given
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            Long userId =
                    tx.execute(status -> saveUser("temperature-delete@test.com").getId());
            Long otherId = tx.execute(status -> userRepository
                    .save(User.builder()
                            .email("temperature-survivor@test.com")
                            .nickname("온도보존사용자")
                            .profile_img("")
                            .password("password")
                            .role(Role.USER)
                            .build())
                    .getId());
            try {
                tx.executeWithoutResult(status -> {
                    temperatureService.record(
                            TemperatureActivityType.FOUND_POST, userId, SOURCE_ID.incrementAndGet(), NOW);
                    temperatureService.record(
                            TemperatureActivityType.FOUND_POST, otherId, SOURCE_ID.incrementAndGet(), NOW);
                    temperatureService.recalculate();
                });

                // when: 사용자 정리 작업과 같은 Repository 삭제를 flush한다.
                tx.executeWithoutResult(status -> {
                    userRepository.deleteById(userId);
                    userRepository.flush();
                    assertThat(jdbcTemplate.queryForObject(
                                    "select count(*) from temperature_activity where user_id=?", Integer.class, userId))
                            .isZero();
                    assertThat(jdbcTemplate.queryForObject(
                                    "select count(*) from user_temperature where user_id=?", Integer.class, userId))
                            .isZero();
                    if (rollback) {
                        status.setRollbackOnly();
                    }
                });

                // then: 삭제 트랜잭션과 다른 영속성 컨텍스트에서 확인한다.
                tx.executeWithoutResult(status -> {
                    assertThat(userRepository.existsById(userId)).isEqualTo(rollback);
                    assertThat(userTemperatureRepository.existsById(userId)).isEqualTo(rollback);
                    assertThat(jdbcTemplate.queryForObject(
                                    "select count(*) from temperature_activity where user_id=?", Integer.class, userId))
                            .isEqualTo(rollback ? 1 : 0);
                    assertThat(userRepository.existsById(otherId)).isTrue();
                    assertThat(userTemperatureRepository
                                    .findById(otherId)
                                    .orElseThrow()
                                    .getTemperatureTenths())
                            .isEqualTo(370);
                    assertThat(jdbcTemplate.queryForObject(
                                    "select count(*) from temperature_activity where user_id=?",
                                    Integer.class,
                                    otherId))
                            .isEqualTo(1);
                });
                if (!rollback) {
                    tx.executeWithoutResult(status -> temperatureService.recalculate());
                    assertThat(userTemperatureRepository.existsById(userId)).isFalse();
                }
            } finally {
                tx.executeWithoutResult(status -> userRepository.deleteAllById(List.of(userId, otherId)));
            }
        }

        @Test
        @DisplayName("온도 원장과 집계만 삭제해도 사용자 행은 삭제하지 않는다")
        void itDoesNotCascadeTemperatureDeletionToUser() {
            // given
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            Long userId = tx.execute(
                    status -> saveUser("temperature-child-delete@test.com").getId());
            long sourceId = SOURCE_ID.incrementAndGet();
            try {
                tx.executeWithoutResult(status -> {
                    temperatureService.record(TemperatureActivityType.FOUND_POST, userId, sourceId, NOW);
                    temperatureService.recalculate();
                });
                Long activityId = jdbcTemplate.queryForObject(
                        "select id from temperature_activity where user_id=? and source_id=?",
                        Long.class,
                        userId,
                        sourceId);

                // when
                tx.executeWithoutResult(status -> {
                    userTemperatureRepository.deleteById(userId);
                    temperatureActivityRepository.deleteById(activityId);
                });

                // then
                assertThat(userRepository.existsById(userId)).isTrue();
                assertThat(userTemperatureRepository.existsById(userId)).isFalse();
                assertThat(temperatureActivityRepository.existsById(activityId)).isFalse();
            } finally {
                tx.executeWithoutResult(status -> userRepository.deleteById(userId));
            }
        }
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .profile_img("")
                .nickname("온도사용자")
                .email(email)
                .password("password")
                .role(Role.USER)
                .build());
    }

    private void saveActivities(User user, TemperatureActivityType type, int count, LocalDateTime occurredAt) {
        for (int i = 0; i < count; i++) {
            temperatureActivityRepository.save(TemperatureActivity.builder()
                    .user(user)
                    .activityType(type)
                    .sourceId(SOURCE_ID.incrementAndGet())
                    .occurredAt(occurredAt)
                    .build());
        }
        temperatureActivityRepository.flush();
    }
}
