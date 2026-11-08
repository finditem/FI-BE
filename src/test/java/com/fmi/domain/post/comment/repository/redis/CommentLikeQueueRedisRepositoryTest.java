package com.fmi.domain.post.comment.repository.redis;

import static org.assertj.core.api.Assertions.*;

import com.fmi.support.IntegrationTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("CommentLikeQueueRedisRepository")
class CommentLikeQueueRedisRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private CommentLikeQueueRedisRepository repository;

    @BeforeEach
    @AfterEach
    void clearOwnedKeys() {
        var keys = redis.keys("comment:like:queue:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Nested
    @DisplayName("저장 상태를 처리할 때")
    class DescribeStorage {
        @Test
        @DisplayName("알림 대기열 조회는 데이터를 소비하지 않고 명시적 삭제로만 제거한다")
        void readsWithoutConsumingQueue() {
            // given
            redis.opsForSet().add("comment:like:queue:1", "_init", "42");
            // when
            var users = repository.findUserIds(1L);
            // then
            assertThat(users).containsExactlyInAnyOrder("_init", "42");
            users.remove("_init");
            assertThat(repository.findUserIds(1L)).containsExactlyInAnyOrder("_init", "42");
            assertThat(redis.getExpire("comment:like:queue:1")).isEqualTo(-1L);
            repository.delete(1L);
            assertThat(redis.hasKey("comment:like:queue:1")).isFalse();
        }
    }
}
