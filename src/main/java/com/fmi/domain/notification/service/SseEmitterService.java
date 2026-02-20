package com.fmi.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    private static final Long DEFAULT_TIMEOUT = 60 * 60 * 1000L; // 1시간
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 기존 연결이 있으면 제거
        SseEmitter oldEmitter = emitters.remove(userId);
        if (oldEmitter != null) {
            log.info("기존 SSE 연결 종료: userId={}", userId);
            oldEmitter.complete();
        }

        // 새 연결 등록
        emitters.put(userId, emitter);
        log.info("SSE 연결 등록: userId={}, 현재 연결 수={}", userId, emitters.size());

        // 연결 완료 시 제거
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            emitters.remove(userId);
        });

        // 타임아웃 시 제거
        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃: userId={}", userId);
            emitters.remove(userId);
        });

        // 에러 발생 시 제거
        emitter.onError(e -> {
            log.error("SSE 에러 발생: userId={}, error={}", userId, e.getMessage());
            emitters.remove(userId);
        });

        // 연결 확인용 초기 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결 성공"));
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패: userId={}", userId, e);
            emitters.remove(userId);
            throw new RuntimeException("SSE 연결 실패", e);
        }

        return emitter;
    }

    public void sendToUser(Long userId, Object data) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            log.debug("SSE 연결 없음: userId={}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(data));
            log.info("SSE 알림 전송 성공: userId={}", userId);
        } catch (IOException e) {
            log.error("SSE 알림 전송 실패: userId={}", userId, e);
            emitters.remove(userId);
            emitter.completeWithError(e);
        }
    }

    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            log.info("SSE 연결 해제: userId={}", userId);
            emitter.complete();
        }
    }

    public int getConnectionCount() {
        return emitters.size();
    }

    public boolean isConnected(Long userId) {
        return emitters.containsKey(userId);
    }
}
