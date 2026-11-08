package com.fmi.domain.chat.room.service;

import static org.assertj.core.api.Assertions.*;

import com.fmi.support.IntegrationTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("ChatRoomPresenceService")
class ChatRoomPresenceServiceTest extends IntegrationTestSupport {
    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ChatRoomPresenceService service;

    @BeforeEach
    @AfterEach
    void clearOwnedKeys() {
        var keys = redis.keys("presence:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Nested
    @DisplayName("저장 상태를 처리할 때")
    class DescribeStorage {
        @Test
        @DisplayName("입장 시 네 키를 갱신하고 연결 종료 시 방과 세션 정보를 정리한다")
        void enterAndDisconnect() {
            // given
            // when
            service.enter(1L, 2L, "session");
            service.enter(3L, 2L, "session");
            // then
            assertThat(service.isUserPresent(1L, 2L)).isTrue();
            for (String key : new String[] {
                "presence:room:1", "presence:user:2", "presence:sess:session", "presence:sess:session:rooms"
            }) {
                assertThat(redis.getExpire(key)).isBetween(40L, 45L);
            }
            service.disconnect("session");
            assertThat(service.isUserPresent(1L, 2L)).isFalse();
            assertThat(service.isUserPresent(3L, 2L)).isFalse();
            assertThat(redis.hasKey("presence:sess:session")).isFalse();
            assertThat(redis.hasKey("presence:sess:session:rooms")).isFalse();
            assertThat(redis.opsForSet().members("presence:room:1")).isEmpty();
        }

        @Test
        @DisplayName("입장 중 타입 오류가 발생하면 앞선 방 등록만 남고 TTL은 설정되지 않는다")
        void failedEnterKeepsEarlierWrite() {
            // given
            redis.opsForValue().set("presence:user:5", "wrong-type");
            // when
            Throwable failure = catchThrowable(() -> service.enter(4L, 5L, "failed"));
            // then
            assertThat(failure).isNotNull();
            assertThat(redis.opsForSet().members("presence:room:4")).containsExactly("5");
            assertThat(redis.getExpire("presence:room:4")).isEqualTo(-1L);
            assertThat(redis.hasKey("presence:sess:failed")).isFalse();
        }
    }
}
