package com.fmi.domain.notification.service;

import com.fmi.domain.notification.converter.NotificationConverter;
import com.fmi.domain.notification.data.Notification;
import com.fmi.domain.notification.data.NotificationSettings;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.repository.NotificationRepository;
import com.fmi.domain.notification.repository.NotificationSettingsRepository;
import com.fmi.domain.user.user.data.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationDeliveryService {

    private final NotificationRepository notificationRepository;

    private final NotificationSettingsRepository notificationSettingsRepository;

    private final NotificationConverter notificationConverter;

    private final SseEmitterService sseEmitterService;

    private final WebPushService webPushService;

    /**
     * 알림 생성 (내부 사용 - 다른 Service에서 호출)
     */
    @Transactional
    public void createNotification(
            User user,
            NotificationType type,
            String title,
            String message,
            ReferenceType referenceType,
            Long referenceId) {
        createNotification(user, type, title, message, referenceType, referenceId, null);
    }

    @Transactional
    public void createNotification(
            User user,
            NotificationType type,
            String title,
            String message,
            ReferenceType referenceType,
            Long referenceId,
            Long roomId) {
        // 알림 설정 확인
        NotificationSettings settings =
                notificationSettingsRepository.findByUser(user).orElse(null);

        // 설정이 없거나 해당 타입 알림이 켜져있으면 생성
        if (settings == null || isNotificationEnabled(settings, type)) {
            Notification notification = Notification.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .message(message)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .roomId(roomId)
                    .build();

            notificationRepository.save(notification);

            log.info(" 알림 저장 완료: {}", notification.getNotificationId());

            // 트랜잭션 커밋 이후 SSE 전송 (실패 시에도 DB 저장은 보장)
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            // SSE로 실시간 푸시
                            sseEmitterService.sendToUser(user.getId(), notificationConverter.toListDTO(notification));
                            log.info(
                                    "SSE 알림 전송 시도: userId={}, notificationId={}",
                                    user.getId(),
                                    notification.getNotificationId());
                        } catch (Exception e) {
                            log.warn("SSE 알림 전송 실패 (SSE 미연결 가능성): userId={}", user.getId(), e);
                        }
                        // Web Push 발송
                        try {
                            String url = buildNotificationUrl(referenceType, referenceId);
                            webPushService.sendPush(user, title, message, url, type.name());
                        } catch (Exception e) {
                            log.warn("Web Push 발송 실패: userId={}", user.getId(), e);
                        }
                    }
                });
            }
        }
    }

    /**
     * 알림 딥링크 URL 생성
     */
    private String buildNotificationUrl(ReferenceType referenceType, Long referenceId) {
        if (referenceType == null || referenceId == null) return "/";
        return switch (referenceType) {
            case POST -> "/posts/" + referenceId;
            case COMMENT -> "/posts/" + referenceId;
            case CHAT -> "/chat/" + referenceId;
            case INQUIRY -> "/inquiry/" + referenceId;
            case NOTICE -> "/notice/" + referenceId;
            case REPORT -> "/report/" + referenceId;
            case REVIEW -> "/mypage/reviews/" + referenceId;
        };
    }

    /**
     * 새로운 알림 갱신 (채팅 알림 갱신용)
     */
    @Transactional
    public void updateChatNotification(Notification notification, String newMessage) {
        notification.updateContent(newMessage);
        log.info(" 알림 갱신 완료: {}", notification.getNotificationId());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        // SSE로 실시간 푸시
                        sseEmitterService.sendToUser(
                                notification.getUser().getId(), notificationConverter.toListDTO(notification));
                        log.info(
                                "SSE 알림 갱신 전송: userId={}, notificationId={}",
                                notification.getUser().getId(),
                                notification.getNotificationId());
                    } catch (Exception e) {
                        log.warn(
                                "SSE 알림 갱신 전송 실패: userId={}",
                                notification.getUser().getId(),
                                e);
                    }
                    // Web Push 발송
                    try {
                        String url =
                                buildNotificationUrl(notification.getReferenceType(), notification.getReferenceId());
                        webPushService.sendPush(
                                notification.getUser(),
                                notification.getTitle(),
                                newMessage,
                                url,
                                notification.getType().name());
                    } catch (Exception e) {
                        log.warn(
                                "Web Push 갱신 발송 실패: userId={}",
                                notification.getUser().getId(),
                                e);
                    }
                }
            });
        }
    }

    /**
     * 알림 타입별 설정 확인
     */
    private boolean isNotificationEnabled(NotificationSettings settings, NotificationType type) {
        switch (type) {
            case COMMENT:
                return Boolean.TRUE.equals(settings.getCommentEnabled());
            case CHAT:
                return Boolean.TRUE.equals(settings.getChatEnabled());
            case INQUIRY_REPLY:
                return Boolean.TRUE.equals(settings.getInquiryReplyEnabled());
            case REPORT_RESULT:
                return Boolean.TRUE.equals(settings.getReportResultEnabled());
            case FAVORITE:
                return Boolean.TRUE.equals(settings.getFavoriteEnabled());
            case NOTICE:
                return Boolean.TRUE.equals(settings.getNoticeEnabled());
            case CATEGORY:
                return Boolean.TRUE.equals(settings.getCategoryEnabled());
            default:
                return true;
        }
    }
}
