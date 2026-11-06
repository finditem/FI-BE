package com.fmi.domain.post.comment.repository.redis;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentLikeQueueRedisRepository {
    private final StringRedisTemplate redis;

    public Set<String> findUserIds(Long commentId) {
        Set<String> userIds = redis.opsForSet().members("comment:like:queue:" + commentId);
        return userIds;
    }

    public void delete(Long commentId) {
        redis.delete("comment:like:queue:" + commentId);
    }
}
