package com.fmi.domain.chatmessage;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 메시지 컨텍스트 유지
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        // WebSocket CONNECT 요청에서 Authorization 헤더 추출
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearerToken = accessor.getFirstNativeHeader("Authorization");

            // Authorization 헤더 존재 여부 및 형식 검증
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                log.warn("STOMP 인증 실패: Authorization 헤더가 없거나 형식이 잘못되었습니다.");
                throw new GeneralException(ErrorStatus._TOKEN_NOT_FOUND);
            }

            String token = bearerToken.substring(7);

            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                String role = jwtTokenProvider.getRole(token);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        String.valueOf(userId),
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

                // SecurityContext에 Authentication 설정
                //SecurityContextHolder.getContext().setAuthentication(authentication);

                // Principal 설정 추가
                accessor.setUser(authentication);
                log.info("STOMP 인증 성공! User: {}", userId);

            } else {
                throw new GeneralException(ErrorStatus._INVALID_TOKEN);
            }
        }

        return message;
    }

}
