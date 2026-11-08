package com.fmi.domain.inquiry.inquiry.repository.redis;

import static org.assertj.core.api.Assertions.*;

import com.fmi.support.IntegrationTestSupport;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("InquiryGuestRequestRedisRepository")
class InquiryGuestRequestRedisRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private InquiryGuestRequestRedisRepository repository;

    @BeforeEach
    @AfterEach
    void clearOwnedKeys() {
        var keys = redis.keys("inquiry:guest:count:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Nested
    @DisplayName("저장 상태를 처리할 때")
    class DescribeStorage {
        @Test
        @DisplayName("첫 요청만 만료 시간을 설정하고 삭제 전까지 횟수를 보존한다")
        void countsWithinOriginalWindow() {
            // given
            String ip = "test-ip";
            // when
            Long first = repository.incrementCount(ip);
            // then
            assertThat(first).isEqualTo(1L);
            assertThat(redis.getExpire("inquiry:guest:count:" + ip)).isBetween(175L, 180L);
            redis.expire("inquiry:guest:count:" + ip, Duration.ofSeconds(60));
            assertThat(repository.incrementCount(ip)).isEqualTo(2L);
            assertThat(redis.getExpire("inquiry:guest:count:" + ip)).isBetween(1L, 60L);
            repository.deleteCount(ip);
            assertThat(redis.hasKey("inquiry:guest:count:" + ip)).isFalse();
        }
    }
}
