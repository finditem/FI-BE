package com.fmi.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    private static final Long DEFAULT_TIMEOUT = 60 * 60 * 1000L; // 1시간
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 유저의 emitter 리스트에 추가 (다중 기기 지원)
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE 연결 등록: userId={}, 해당 유저 연결 수={}, 전체 유저 수={}",
                userId, emitters.get(userId).size(), emitters.size());

        // 연결 완료 시 제거
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            removeEmitter(userId, emitter);
        });

        // 타임아웃 시 제거
        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃: userId={}", userId);
            removeEmitter(userId, emitter);
        });

        // 에러 발생 시 제거
        emitter.onError(e -> {
            log.error("SSE 에러 발생: userId={}, error={}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        // 연결 확인용 초기 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결 성공"));
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패: userId={}", userId, e);
            removeEmitter(userId, emitter);
            throw new RuntimeException("SSE 연결 실패", e);
        }

        return emitter;
    }

    public void sendToUser(Long userId, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("SSE 연결 없음: userId={}", userId);
            return;
        }

        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
            } catch (IOException e) {
                log.error("SSE 알림 전송 실패: userId={}", userId, e);
                deadEmitters.add(emitter);
            }
        }

        // 전송 실패한 emitter 정리
        for (SseEmitter dead : deadEmitters) {
            removeEmitter(userId, dead);
            dead.completeWithError(new IOException("전송 실패"));
        }

        if (!deadEmitters.isEmpty()) {
            log.info("SSE 알림 전송: userId={}, 성공={}, 실패={}",
                    userId, userEmitters.size() - deadEmitters.size(), deadEmitters.size());
        } else {
            log.info("SSE 알림 전송 성공: userId={}, 연결 수={}", userId, userEmitters.size());
        }
    }

    public void sendUnreadNotifications(Long userId, List<?> unreadNotifications) {
        if (unreadNotifications == null || unreadNotifications.isEmpty()) {
            return;
        }

        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        // 가장 최근 연결된 emitter에만 미읽은 알림 전송 (중복 방지)
        SseEmitter latestEmitter;
        try {
            latestEmitter = userEmitters.get(userEmitters.size() - 1);
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        for (Object notification : unreadNotifications) {
            try {
                latestEmitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                log.error("SSE 미읽은 알림 전송 실패: userId={}", userId, e);
                removeEmitter(userId, latestEmitter);
                latestEmitter.completeWithError(e);
                return;
            }
        }
        log.info("SSE 미읽은 알림 {} 건 전송: userId={}", unreadNotifications.size(), userId);
    }

    public void disconnect(Long userId) {
        List<SseEmitter> userEmitters = emitters.remove(userId);
        if (userEmitters != null) {
            log.info("SSE 연결 전체 해제: userId={}, 해제 수={}", userId, userEmitters.size());
            for (SseEmitter emitter : userEmitters) {
                emitter.complete();
            }
        }
    }

    public int getConnectionCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    public boolean isConnected(Long userId) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters != null && !userEmitters.isEmpty();
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (key, userEmitters) -> {
            userEmitters.remove(emitter);
            return userEmitters.isEmpty() ? null : userEmitters;
        });
    }
}
