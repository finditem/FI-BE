package com.fmi.domain.post.post.service;

import static org.assertj.core.api.Assertions.*;

import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.support.IntegrationTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("PostUpdateRateLimiter")
class PostUpdateRateLimiterTest extends IntegrationTestSupport {
    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private PostUpdateRateLimiter limiter;

    @BeforeEach
    @AfterEach
    void clearOwnedKeys() {
        var keys = redis.keys("post:update:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Nested
    @DisplayName("저장 상태를 처리할 때")
    class DescribeStorage {
        @Test
        @DisplayName("여섯 번째 수정은 카운터를 삭제하고 잠금을 남기며 재요청은 새 카운터를 만들지 않는다")
        void sixthUpdateLocksWithoutRecounting() {
            // given
            for (int i = 0; i < 5; i++) {
                limiter.checkAndRecord(1L);
            }
            assertThat(redis.getExpire("post:update:count:1")).isBetween(55L, 60L);
            // when
            Throwable failure = catchThrowable(() -> limiter.checkAndRecord(1L));
            // then
            assertThat(failure).isInstanceOf(GeneralException.class);
            assertThat(redis.hasKey("post:update:count:1")).isFalse();
            assertThat(redis.opsForValue().get("post:update:lock:1")).isEqualTo("1");
            assertThat(redis.getExpire("post:update:lock:1")).isBetween(175L, 180L);
            assertThatThrownBy(() -> limiter.checkAndRecord(1L)).isInstanceOf(GeneralException.class);
            assertThat(redis.hasKey("post:update:count:1")).isFalse();
        }
    }
}
