package com.fmi.domain.temperature.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.temperature.data.TemperatureActivity;
import com.fmi.domain.temperature.data.TemperatureActivityType;
import com.fmi.domain.temperature.data.TemperatureSnapshot;
import com.fmi.domain.temperature.repository.TemperatureActivityRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("TemperatureQueryService")
class TemperatureQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private TemperatureQueryService temperatureQueryService;

    @Autowired
    private TemperatureService temperatureService;

    @Autowired
    private TemperatureActivityRepository temperatureActivityRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("내 찾길 온도를 조회할 때")
    class DescribeGetMyTemperature {

        @Test
        @DisplayName("계산된 적 없으면 36.5를 반환한다")
        void itReturnsInitialWithoutCalculation() {
            // given
            User user = saveUser("query-initial@test.com");

            // when
            TemperatureSnapshot result = temperatureQueryService.getMyTemperature(user.getEmail());

            // then
            assertThat(result.temperature().celsius()).isEqualByComparingTo(new BigDecimal("36.5"));
            assertThat(result.calculatedAt()).isNull();
        }

        @Test
        @DisplayName("활동이 있어도 정기 계산 전에는 직전 값을 반환한다")
        void itReturnsLastCalculatedValue() {
            // given
            User user = saveUser("query-before@test.com");
            temperatureActivityRepository.saveAndFlush(TemperatureActivity.builder()
                    .userId(user.getId())
                    .activityType(TemperatureActivityType.FOUND_POST)
                    .sourceId(980001L)
                    .occurredAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                    .build());

            // when
            TemperatureSnapshot result = temperatureQueryService.getMyTemperature(user.getEmail());

            // then
            assertThat(result.temperature().celsius()).isEqualByComparingTo(new BigDecimal("36.5"));
        }

        @Test
        @DisplayName("정기 계산 뒤에는 소수 첫째 자리까지 반영된 온도를 반환한다")
        void itReturnsCalculatedValue() {
            // given
            User user = saveUser("query-after@test.com");
            temperatureActivityRepository.saveAndFlush(TemperatureActivity.builder()
                    .userId(user.getId())
                    .activityType(TemperatureActivityType.FOUND_POST)
                    .sourceId(980002L)
                    .occurredAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                    .build());
            temperatureService.recalculate();

            // when
            TemperatureSnapshot result = temperatureQueryService.getMyTemperature(user.getEmail());

            // then
            assertThat(result.temperature().celsius()).isEqualTo(new BigDecimal("37.0"));
            assertThat(result.calculatedAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 12, 0));
        }
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .nickname("조회사용자")
                .email(email)
                .password("password")
                .role(Role.USER)
                .build());
    }
}
