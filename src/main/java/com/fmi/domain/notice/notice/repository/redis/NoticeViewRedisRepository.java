package com.fmi.domain.notice.notice.repository.redis;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NoticeViewRedisRepository {
    private final StringRedisTemplate redis;

    public void initializeCount(Long noticeId, int count) {
        redis.opsForValue().setIfAbsent("notice:view:count:" + noticeId, String.valueOf(count), Duration.ofMinutes(5));
    }

    public boolean addViewer(Long noticeId, String identifier) {
        Long added = redis.opsForSet().add("notice:view:set:" + noticeId, identifier);
        return added != null && added > 0;
    }

    public void incrementCount(Long noticeId) {
        redis.opsForValue().increment("notice:view:count:" + noticeId);
    }

    public void refreshViewers(Long noticeId) {
        redis.expire("notice:view:set:" + noticeId, Duration.ofMinutes(5));
    }

    public Optional<Long> findCount(Long noticeId) {
        return Optional.ofNullable(redis.opsForValue().get("notice:view:count:" + noticeId))
                .map(Long::parseLong);
    }
}
