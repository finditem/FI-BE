package com.fmi.domain.notice.notice.repository.redis;

import static org.assertj.core.api.Assertions.*;

import com.fmi.support.IntegrationTestSupport;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("NoticeViewRedisRepository")
class NoticeViewRedisRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private NoticeViewRedisRepository repository;

    @BeforeEach
    @AfterEach
    void clearOwnedKeys() {
        var keys = redis.keys("notice:view:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Nested
    @DisplayName("저장 상태를 처리할 때")
    class DescribeStorage {
        @Test
        @DisplayName("조회자 중복을 구분하고 초기 조회수와 기존 TTL을 덮어쓰지 않는다")
        void preservesCountAndViewerWindow() {
            // given
            repository.initializeCount(1L, 10);
            redis.expire("notice:view:count:1", Duration.ofSeconds(60));
            // when
            repository.initializeCount(1L, 999);
            boolean added = repository.addViewer(1L, "viewer");
            repository.incrementCount(1L);
            repository.refreshViewers(1L);
            // then
            assertThat(added).isTrue();
            assertThat(repository.addViewer(1L, "viewer")).isFalse();
            assertThat(repository.findCount(1L)).contains(11L);
            assertThat(redis.getExpire("notice:view:count:1")).isBetween(1L, 60L);
            assertThat(redis.getExpire("notice:view:set:1")).isBetween(295L, 300L);
            assertThat(repository.findCount(2L)).isEmpty();
        }

        @Test
        @DisplayName("조회자 기록이 실패해도 앞서 초기화한 조회수는 남는다")
        void failedViewerWriteKeepsInitializedCount() {
            // given
            redis.opsForValue().set("notice:view:set:3", "wrong-type");
            // when
            Throwable failure = catchThrowable(() -> {
                repository.initializeCount(3L, 10);
                repository.addViewer(3L, "viewer");
                repository.incrementCount(3L);
            });
            // then
            assertThat(failure).isNotNull();
            assertThat(repository.findCount(3L)).contains(10L);
        }
    }
}
