package com.fmi.domain.chat.room.repository.redis;

import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatRoomPresenceRedisRepository {
    private final StringRedisTemplate redis;

    private static final Duration TTL = Duration.ofSeconds(45);

    public void addRoomUser(Long roomId, String userId) {
        redis.opsForSet().add("presence:room:" + roomId, userId);
    }

    public void addUserRoom(Long userId, String roomId) {
        redis.opsForSet().add("presence:user:" + userId, roomId);
    }

    public void saveSessionUser(String sessionId, String userId) {
        redis.opsForHash().put("presence:sess:" + sessionId, "userId", userId);
    }

    public void addSessionRoom(String sessionId, String roomId) {
        redis.opsForSet().add("presence:sess:" + sessionId + ":rooms", roomId);
    }

    public void removeRoomUser(Long roomId, String userId) {
        redis.opsForSet().remove("presence:room:" + roomId, userId);
    }

    public void removeUserRoom(Long userId, String roomId) {
        redis.opsForSet().remove("presence:user:" + userId, roomId);
    }

    public void removeSessionRoom(String sessionId, String roomId) {
        redis.opsForSet().remove("presence:sess:" + sessionId + ":rooms", roomId);
    }

    public String findSessionUser(String sessionId) {
        return (String) redis.opsForHash().get("presence:sess:" + sessionId, "userId");
    }

    public Set<String> findSessionRooms(String sessionId) {
        return redis.opsForSet().members("presence:sess:" + sessionId + ":rooms");
    }

    public void deleteSession(String sessionId) {
        redis.delete("presence:sess:" + sessionId);
        redis.delete("presence:sess:" + sessionId + ":rooms");
    }

    public boolean isUserPresent(Long roomId, Long userId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember("presence:user:" + userId, roomId.toString()));
    }

    public void refreshTtl(String sessionId, Long roomId, Long userId) {
        redis.expire("presence:room:" + roomId, TTL);
        redis.expire("presence:user:" + userId, TTL);
        redis.expire("presence:sess:" + sessionId, TTL);
        redis.expire("presence:sess:" + sessionId + ":rooms", TTL);
    }
}
