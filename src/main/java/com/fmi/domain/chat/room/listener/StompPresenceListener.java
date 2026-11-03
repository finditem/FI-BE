package com.fmi.domain.chat.room.listener;

import com.fmi.domain.chat.room.service.ChatRoomPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class StompPresenceListener {
    private final ChatRoomPresenceService presence;

    /**
     * 비정상 세션 종료
     */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent e) {
        presence.disconnect(e.getSessionId());
    }
}
