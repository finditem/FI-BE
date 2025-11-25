package com.fmi.domain.notification.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.notification.converter.NotificationConverter;
import com.fmi.domain.notification.data.Notification;
import com.fmi.domain.notification.data.NotificationSettings;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.repository.NotificationRepository;
import com.fmi.domain.notification.repository.NotificationSettingsRepository;
import com.fmi.domain.notification.web.dto.request.NotificationSettingsUpdateDTO;
import com.fmi.domain.notification.web.dto.response.NotificationListDTO;
import com.fmi.domain.notification.web.dto.response.NotificationSettingsDTO;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.user.repository.UserCategoryRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final NotificationConverter notificationConverter;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    
    /**
     * 내 알림 목록 조회
     */
    public Page<NotificationListDTO> getMyNotifications(User user, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> notifications;
        
        if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByUserAndIsReadFalse(user, pageable);
        } else {
            notifications = notificationRepository.findByUser(user, pageable);
        }
        
        return notifications.map(notificationConverter::toListDTO);
    }
    
    /**
     * 읽지 않은 알림 개수
     */
    public Long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
    
    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_NOT_FOUND));
        
        // 본인 알림인지 확인
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorStatus.NOTIFICATION_ACCESS_DENIED);
        }
        
        notification.markAsRead();
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsRead(user);
    }
    
    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_NOT_FOUND));
        
        // 본인 알림인지 확인
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorStatus.NOTIFICATION_ACCESS_DENIED);
        }
        
        notificationRepository.delete(notification);
    }
    
    /**
     * 모든 알림 삭제
     */
    @Transactional
    public void deleteAllNotifications(User user) {
        notificationRepository.deleteByUser(user);
    }

    /**
     * 알림 다건 읽음 처리
     */
    @Transactional
    public int markAsReadBatch(User user, java.util.List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return 0;
        java.util.List<Notification> list = notificationRepository.findAllById(notificationIds);
        int updated = 0;
        for (Notification n : list) {
            if (n.getUser().getId().equals(user.getId()) && !Boolean.TRUE.equals(n.getIsRead())) {
                n.markAsRead();
                updated++;
            }
        }
        if (updated > 0) notificationRepository.saveAll(list);
        return updated;
    }

    /**
     * 알림 다건 삭제
     */
    @Transactional
    public int deleteBatch(User user, java.util.List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return 0;
        java.util.List<Notification> list = notificationRepository.findAllById(notificationIds);
        int deleted = 0;
        java.util.List<Notification> toDelete = new java.util.ArrayList<>();
        for (Notification n : list) {
            if (n.getUser().getId().equals(user.getId())) {
                toDelete.add(n);
                deleted++;
            }
        }
        if (!toDelete.isEmpty()) notificationRepository.deleteAll(toDelete);
        return deleted;
    }
    
    /**
     * 알림 설정 조회
     */
    public NotificationSettingsDTO getSettings(User user) {
        NotificationSettings settings = notificationSettingsRepository.findByUser(user)
                .orElseGet(() -> createDefaultSettings(user));
        
        return notificationConverter.toSettingsDTO(settings);
    }
    
    /**
     * 알림 설정 변경
     */
    @Transactional
    public NotificationSettingsDTO updateSettings(User user, NotificationSettingsUpdateDTO request) {
        NotificationSettings settings = notificationSettingsRepository.findByUser(user)
                .orElseGet(() -> createDefaultSettings(user));
        
        settings.updateSettings(
                request.getCommentEnabled(),
                request.getChatEnabled(),
                request.getInquiryReplyEnabled(),
                request.getReportResultEnabled(),
                request.getFavoriteEnabled(),
                request.getNoticeEnabled(),
                request.getCategoryEnabled()
        );
        
        NotificationSettings saved = notificationSettingsRepository.save(settings);
        return notificationConverter.toSettingsDTO(saved);
    }

    /**
     * 댓글/채팅/카테고리 설정만 부분 업데이트
     */
    @Transactional
    public NotificationSettingsDTO updateBasicSettings(User user, Boolean commentEnabled, Boolean chatEnabled, Boolean categoryEnabled) {
        NotificationSettings settings = notificationSettingsRepository.findByUser(user)
                .orElseGet(() -> createDefaultSettings(user));
        settings.updateSettings(commentEnabled, chatEnabled, null, null, null, null, categoryEnabled);
        NotificationSettings saved = notificationSettingsRepository.save(settings);
        return notificationConverter.toSettingsDTO(saved);
    }
    
    /**
     * 기본 알림 설정 생성
     */
    @Transactional
    public NotificationSettings createDefaultSettings(User user) {
        NotificationSettings settings = NotificationSettings.builder()
                .user(user)
                .build();
        
        return notificationSettingsRepository.save(settings);
    }
    
    /**
     * 알림 생성 (내부 사용 - 다른 Service에서 호출)
     */
    @Transactional
    public void createNotification(User user, NotificationType type, String title,
                                   String message, ReferenceType referenceType, Long referenceId) {
        // 알림 설정 확인
        NotificationSettings settings = notificationSettingsRepository.findByUser(user).orElse(null);
        
        // 설정이 없거나 해당 타입 알림이 켜져있으면 생성
        if (settings == null || isNotificationEnabled(settings, type)) {
            Notification notification = Notification.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .message(message)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .build();
            
            notificationRepository.save(notification);

            log.info(" 알림 저장 완료: {}", notification.getNotificationId());

            // 트랜잭션 커밋 이후 웹소켓 전송 (실패 시에도 DB 저장은 보장)
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            // 사용자별 큐로 푸시
                            simpMessagingTemplate.convertAndSendToUser(
                                    user.getId().toString(),
                                    "/queue/notification",
                                    notificationConverter.toListDTO(notification)
                            );
                        } catch (Exception ignored) {}
                    }
                });
            }
        }
    }

    /**
     * 공지 브로드캐스트: 설정이 켜진 사용자에게만 생성/전송
     */
    @Transactional
    public void broadcastNotice(String title, String message, Long noticeId) {
        // 활성 사용자만 조회 (삭제된 사용자 제외)
        java.util.List<com.fmi.domain.auth.data.User> users = userRepository.findAllActiveUsers();
        for (com.fmi.domain.auth.data.User user : users) {
            NotificationSettings settings = notificationSettingsRepository.findByUser(user).orElse(null);
            if (settings == null || Boolean.TRUE.equals(settings.getNoticeEnabled())) {
                createNotification(user, NotificationType.NOTICE, title, message, ReferenceType.NOTICE, noticeId);
            }
        }
    }

    /**
     * 게시글 생성 시 카테고리(Type) 기준 알림
     * - 임시 정책: 동일 카테고리를 구독한 사용자에게 1회 발송
     */
    @Transactional
    public void notifyCategoriesForPost(Post post) {
        long subscribedCount = userCategoryRepository.count();
        if (subscribedCount == 0) {
            return;
        }
        // TODO: 게시글 Category 필드가 도입되면 구독 사용자 선별 및 알림 전송 로직을 구현합니다.
        log.debug("카테고리 기반 알림은 게시글 Category 연동 후 지원됩니다. postId={}, subscribedCount={}", post.getId(), subscribedCount);
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
                        simpMessagingTemplate.convertAndSendToUser(
                                notification.getUser().getId().toString(),
                                "/queue/notification",
                                notificationConverter.toListDTO(notification)
                        );
                    } catch (Exception ignored) {}
                }
            });
        }
    }
    
}

