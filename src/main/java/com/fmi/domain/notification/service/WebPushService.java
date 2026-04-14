package com.fmi.domain.notification.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notification.data.PushSubscription;
import com.fmi.domain.notification.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Value("${VAPID_PUBLIC_KEY:}")
    private String vapidPublicKey;

    @Value("${VAPID_PRIVATE_KEY:}")
    private String vapidPrivateKey;

    @Value("${VAPID_SUBJECT:mailto:admin@findmyitem.co.kr}")
    private String vapidSubject;

    private PushService pushService;

    @PostConstruct
    public void init() {
        if (vapidPublicKey.isBlank() || vapidPrivateKey.isBlank()) {
            log.warn("VAPID 키가 설정되지 않아 Web Push 비활성화");
            return;
        }
        try {
            Security.addProvider(new BouncyCastleProvider());
            pushService = new PushService()
                    .setPublicKey(vapidPublicKey)
                    .setPrivateKey(vapidPrivateKey)
                    .setSubject(vapidSubject);
            log.info("Web Push 서비스 초기화 완료");
        } catch (GeneralSecurityException e) {
            log.error("Web Push 서비스 초기화 실패", e);
        }
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Async
    public void sendPush(User user, String title, String body, String url, String type) {
        if (pushService == null) {
            return;
        }
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUser(user);
        for (PushSubscription sub : subscriptions) {
            sendToSubscription(sub, title, body, url, type);
        }
    }

    @Async
    public void sendPushToUsers(List<User> users, String title, String body, String url, String type) {
        if (pushService == null) {
            return;
        }
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserIn(users);
        for (PushSubscription sub : subscriptions) {
            sendToSubscription(sub, title, body, url, type);
        }
    }

    private void sendToSubscription(PushSubscription sub, String title, String body, String url, String type) {
        try {
            String payload = String.format(
                    "{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\",\"type\":\"%s\"}",
                    escapeJson(title), escapeJson(body), escapeJson(url), escapeJson(type)
            );

            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload.getBytes()
            );

            var response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 410 || statusCode == 404) {
                log.info("만료된 푸시 구독 삭제: endpoint={}", sub.getEndpoint());
                pushSubscriptionRepository.delete(sub);
            } else if (statusCode >= 400) {
                log.warn("Web Push 발송 실패: statusCode={}, endpoint={}", statusCode, sub.getEndpoint());
            }
        } catch (Exception e) {
            log.error("Web Push 발송 중 오류: endpoint={}", sub.getEndpoint(), e);
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
