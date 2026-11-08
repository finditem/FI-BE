package com.fmi.domain.chat.room.service;

import com.fmi.domain.chat.room.repository.redis.ChatRoomPresenceRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomPresenceService {
    private final ChatRoomPresenceRedisRepository presenceRedisRepository;

    /**
     * 방 입장 (Client가 /app/rooms/{roomId}/enter 호출)
     */
    public void enter(Long roomId, Long userId, String sessionId) {
        presenceRedisRepository.addRoomUser(roomId, userId.toString());
        presenceRedisRepository.addUserRoom(userId, String.valueOf(roomId));
        // 세션 ↔ 방 매핑
        presenceRedisRepository.saveSessionUser(sessionId, userId.toString());
        presenceRedisRepository.addSessionRoom(sessionId, String.valueOf(roomId));
        touchTTL(sessionId, roomId, userId);
    }

    /**
     * 방 퇴장 (Client가 /app/rooms/{roomId}/leave 호출)
     */
    public void leave(Long roomId, Long userId, String sessionId) {
        String sRoomId = String.valueOf(roomId);
        String sUserId = String.valueOf(userId);

        presenceRedisRepository.removeRoomUser(roomId, sUserId);
        presenceRedisRepository.removeUserRoom(userId, sRoomId);
        presenceRedisRepository.removeSessionRoom(sessionId, sRoomId);
    }

    /**
     * 비정상 세션 종료 (Disconnect 이벤트용)
     */
    public void disconnect(String sessionId) {
        String sUserId = presenceRedisRepository.findSessionUser(sessionId);

        if (sUserId != null) {
            Long userId = Long.valueOf(sUserId);
            var rooms = presenceRedisRepository.findSessionRooms(sessionId);
            if (rooms != null) {
                for (String r : rooms) {
                    Long roomId = Long.valueOf(r);
                    presenceRedisRepository.removeRoomUser(roomId, sUserId);
                    presenceRedisRepository.removeUserRoom(userId, r);
                }
            }
        }
        presenceRedisRepository.deleteSession(sessionId);
    }

    /**
     * 유저가 방안에 있는지 확인
     */
    public boolean isUserPresent(Long roomId, Long userId) {
        return presenceRedisRepository.isUserPresent(roomId, userId);
    }

    /**
     * TTL 연장 (Client가 /app/rooms/{roomId}/ping 호출)
     */
    public void touchTTL(String sessionId, Long roomId, Long userId) {
        presenceRedisRepository.refreshTtl(sessionId, roomId, userId);
    }
}
