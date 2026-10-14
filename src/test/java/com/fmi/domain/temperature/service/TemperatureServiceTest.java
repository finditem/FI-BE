package com.fmi.domain.temperature.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.temperature.data.TemperatureActivity;
import com.fmi.domain.temperature.data.TemperatureActivityType;
import com.fmi.domain.temperature.data.UserTemperature;
import com.fmi.domain.temperature.repository.TemperatureActivityRepository;
import com.fmi.domain.temperature.repository.UserTemperatureRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

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
                            .filter(activity -> activity.getUserId().equals(user.getId())))
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
                    .filter(activity -> activity.getUserId().equals(user.getId()))
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

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .nickname("온도사용자")
                .email(email)
                .password("password")
                .role(Role.USER)
                .build());
    }

    private void saveActivities(User user, TemperatureActivityType type, int count, LocalDateTime occurredAt) {
        for (int i = 0; i < count; i++) {
            temperatureActivityRepository.save(TemperatureActivity.builder()
                    .userId(user.getId())
                    .activityType(type)
                    .sourceId(SOURCE_ID.incrementAndGet())
                    .occurredAt(occurredAt)
                    .build());
        }
        temperatureActivityRepository.flush();
    }
}
