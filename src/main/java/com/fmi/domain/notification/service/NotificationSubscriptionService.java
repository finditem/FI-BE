package com.fmi.domain.notification.service;

import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class NotificationSubscriptionService {
    private final UserQueryService userQueryService;
    private final SseEmitterService sseEmitterService;
    private final NotificationService notificationService;

    public SseEmitter subscribe(UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());
        SseEmitter emitter = sseEmitterService.subscribe(user.getId());

        // 재연결 시 미읽은 알림 전송
        sseEmitterService.sendUnreadNotifications(user.getId(), notificationService.getUnreadNotificationDTOs(user));

        return emitter;
    }
}
