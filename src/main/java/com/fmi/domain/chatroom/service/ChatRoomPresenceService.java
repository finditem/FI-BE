package com.fmi.domain.chatroom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatRoomPresenceService {
    private final StringRedisTemplate redis;

    private static final String ROOM_KEY = "presence:room:";
    private static final String USER_KEY = "presence:user:";
    private static final String SESS_KEY = "presence:sess:";
    private static final Duration TTL = Duration.ofSeconds(45);

    private String roomKey(Long roomId) { return ROOM_KEY + roomId; }
    private String userKey(String email) { return USER_KEY + email; }
    private String sessKey(String sessionId){ return SESS_KEY + sessionId; }

    /**
     * 방 입장 (Client가 /app/rooms/{roomId}/enter 호출)
     */
    public void enter(Long roomId, String email, String sessionId) {
        redis.opsForSet().add(roomKey(roomId), email);
        redis.opsForSet().add(userKey(email), String.valueOf(roomId));
        // 세션 ↔ 방 매핑
        redis.opsForHash().put(sessKey(sessionId), "email", email);
        redis.opsForSet().add(sessKey(sessionId) + ":rooms", String.valueOf(roomId));
        touchTTL(sessionId, roomId, email);
    }

    /**
     * 방 퇴장 (Client가 /app/rooms/{roomId}/leave 호출)
     */
    public void leave(Long roomId, String email, String sessionId) {
        String sRoomId = String.valueOf(roomId);
        redis.opsForSet().remove(roomKey(roomId), email);
        redis.opsForSet().remove(userKey(email), sRoomId);
        redis.opsForSet().remove(sessKey(sessionId) + ":rooms", sRoomId);
    }

    /**
     * 비정상 세션 종료 (Disconnect 이벤트용)
     */
    public void disconnect(String sessionId) {
        var h = redis.opsForHash();
        String email = (String) h.get(sessKey(sessionId), "email");

        if (email != null) {
            var rooms = redis.opsForSet().members(sessKey(sessionId) + ":rooms");
            if (rooms != null) {
                for (String r : rooms) {
                    Long roomId = Long.valueOf(r);
                    redis.opsForSet().remove(roomKey(roomId), email);
                    redis.opsForSet().remove(userKey(email), r);
                }
            }
        }
        redis.delete(sessKey(sessionId));
        redis.delete(sessKey(sessionId) + ":rooms");
    }

    /**
     * 유저가 방안에 있는지 확인
     */
    public boolean isUserPresent(Long roomId, String email) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(roomKey(roomId), email));
    }

    /**
     * TTL 연장 (Client가 /app/rooms/{roomId}/ping 호출)
     */
    public void touchTTL(String sessionId, Long roomId, String email) {
        redis.expire(roomKey(roomId), TTL);
        redis.expire(userKey(email), TTL);
        redis.expire(sessKey(sessionId), TTL);
        redis.expire(sessKey(sessionId) + ":rooms", TTL);
    }

}
