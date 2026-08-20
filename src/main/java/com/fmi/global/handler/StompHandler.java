package com.fmi.global.handler;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StompHandler implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> attrs = accessor.getSessionAttributes();
            Authentication auth = (attrs != null) ? (Authentication) attrs.get("AUTH") : null;

            if (auth == null) {
                log.warn("[STOMP] CONNECT denied: no AUTH in session");
                throw new GeneralException(ErrorStatus._TOKEN_NOT_FOUND);
            }

            accessor.setUser(auth);
            log.debug("[STOMP] CONNECT ok. principal={}", auth.getName());
        }

        return message;
    }
}
