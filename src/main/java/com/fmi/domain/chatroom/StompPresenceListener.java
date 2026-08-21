package com.fmi.domain.chatroom;

import com.fmi.domain.chatroom.service.ChatRoomPresenceService;
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
