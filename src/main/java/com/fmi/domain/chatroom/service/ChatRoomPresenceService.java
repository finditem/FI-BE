package com.fmi.domain.chatroom.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomPresenceService {
    private final StringRedisTemplate redis;

    private static final String ROOM_KEY = "presence:room:";
    private static final String USER_KEY = "presence:user:";
    private static final String SESS_KEY = "presence:sess:";
    private static final Duration TTL = Duration.ofSeconds(45);

    private String roomKey(Long roomId) {
        return ROOM_KEY + roomId;
    }

    private String userKey(Long userId) {
        return USER_KEY + userId;
    }

    private String sessKey(String sessionId) {
        return SESS_KEY + sessionId;
    }

    /**
     * 방 입장 (Client가 /app/rooms/{roomId}/enter 호출)
     */
    public void enter(Long roomId, Long userId, String sessionId) {
        redis.opsForSet().add(roomKey(roomId), userId.toString());
        redis.opsForSet().add(userKey(userId), String.valueOf(roomId));
        // 세션 ↔ 방 매핑
        redis.opsForHash().put(sessKey(sessionId), "userId", userId.toString());
        redis.opsForSet().add(sessKey(sessionId) + ":rooms", String.valueOf(roomId));
        touchTTL(sessionId, roomId, userId);
    }

    /**
     * 방 퇴장 (Client가 /app/rooms/{roomId}/leave 호출)
     */
    public void leave(Long roomId, Long userId, String sessionId) {
        String sRoomId = String.valueOf(roomId);
        String sUserId = String.valueOf(userId);

        redis.opsForSet().remove(roomKey(roomId), sUserId);
        redis.opsForSet().remove(userKey(userId), sRoomId);
        redis.opsForSet().remove(sessKey(sessionId) + ":rooms", sRoomId);
    }

    /**
     * 비정상 세션 종료 (Disconnect 이벤트용)
     */
    public void disconnect(String sessionId) {
        var h = redis.opsForHash();
        String sUserId = (String) h.get(sessKey(sessionId), "userId");

        if (sUserId != null) {
            Long userId = Long.valueOf(sUserId);
            var rooms = redis.opsForSet().members(sessKey(sessionId) + ":rooms");
            if (rooms != null) {
                for (String r : rooms) {
                    Long roomId = Long.valueOf(r);
                    redis.opsForSet().remove(roomKey(roomId), sUserId);
                    redis.opsForSet().remove(userKey(userId), r);
                }
            }
        }
        redis.delete(sessKey(sessionId));
        redis.delete(sessKey(sessionId) + ":rooms");
    }

    /**
     * 유저가 방안에 있는지 확인
     */
    public boolean isUserPresent(Long roomId, Long userId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(userKey(userId), roomId.toString()));
    }

    /**
     * TTL 연장 (Client가 /app/rooms/{roomId}/ping 호출)
     */
    public void touchTTL(String sessionId, Long roomId, Long userId) {
        redis.expire(roomKey(roomId), TTL);
        redis.expire(userKey(userId), TTL);
        redis.expire(sessKey(sessionId), TTL);
        redis.expire(sessKey(sessionId) + ":rooms", TTL);
    }
}
