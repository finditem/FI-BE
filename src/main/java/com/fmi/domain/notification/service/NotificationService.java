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
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final NotificationConverter notificationConverter;
    private final SseEmitterService sseEmitterService;
    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    
    /**
     * 내 알림 목록 조회
     */
    public Page<NotificationListDTO> getMyNotifications(User user, Boolean unreadOnly, NotificationType notificationType, Pageable pageable) {
        Page<Notification> notifications;

        if (notificationType != null && Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByUserAndTypeAndIsReadFalse(user, notificationType, pageable);
        } else if (notificationType != null) {
            notifications = notificationRepository.findByUserAndType(user, notificationType, pageable);
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByUserAndIsReadFalse(user, pageable);
        } else {
            notifications = notificationRepository.findByUser(user, pageable);
        }

        return notifications.map(notificationConverter::toListDTO);
    }
    
    /**
     * 내 알림 목록 조회 (커서 기반)
     */
    public CursorPageResponse<NotificationListDTO> getMyNotificationsCursor(User user, Boolean unreadOnly, NotificationType notificationType, Long cursor, int size) {
        int fetchSize = size + 1;
        Pageable limit = PageRequest.of(0, fetchSize);
        List<Notification> notifications;

        if (notificationType != null && Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByUserAndTypeAndIsReadFalseCursor(user, notificationType, cursor, limit);
        } else if (notificationType != null) {
            notifications = notificationRepository.findByUserAndTypeCursor(user, notificationType, cursor, limit);
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByUserAndIsReadFalseCursor(user, cursor, limit);
        } else {
            notifications = notificationRepository.findByUserCursor(user, cursor, limit);
        }

        boolean hasNext = notifications.size() > size;
        List<Notification> content = hasNext ? notifications.subList(0, size) : notifications;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getNotificationId() : null;

        List<NotificationListDTO> responseList = content.stream()
                .map(notificationConverter::toListDTO)
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
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
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTIFICATION_NOT_FOUND));
        
        // 본인 알림인지 확인
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorStatus._NOTIFICATION_ACCESS_DENIED);
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
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTIFICATION_NOT_FOUND));
        
        // 본인 알림인지 확인
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new GeneralException(ErrorStatus._NOTIFICATION_ACCESS_DENIED);
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
    @Transactional
    public NotificationSettingsDTO getSettings(User user) {
        NotificationSettings settings = notificationSettingsRepository.findByUser(user)
                .orElseGet(() -> createDefaultSettings(user));
        
        // User 엔티티를 최신 상태로 조회 (marketingConsent 포함)
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        
        return notificationConverter.toSettingsDTO(settings, freshUser);
    }
    
    /**
     * 알림 설정 변경
     */
    @Transactional
    public NotificationSettingsDTO updateSettings(User user, NotificationSettingsUpdateDTO request) {
        // User 엔티티를 최신 상태로 조회
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        
        NotificationSettings settings = notificationSettingsRepository.findByUser(freshUser)
                .orElseGet(() -> createDefaultSettings(freshUser));
        
        // NotificationSettings 업데이트
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
        
        // User.marketingConsent 업데이트 (요청에 값이 있는 경우에만)
        if (request.getMarketingConsent() != null) {
            freshUser.setMarketingConsent(request.getMarketingConsent());
            userRepository.save(freshUser);
        }
        
        return notificationConverter.toSettingsDTO(saved, freshUser);
    }

    /**
     * 댓글/채팅/카테고리 설정만 부분 업데이트
     * @deprecated 모든 알림 타입을 변경할 수 있는 updateSettings() 사용 권장
     */
    @Deprecated
    @Transactional
    public NotificationSettingsDTO updateBasicSettings(User user, Boolean commentEnabled, Boolean chatEnabled, Boolean categoryEnabled) {
        // User 엔티티를 최신 상태로 조회
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        
        NotificationSettings settings = notificationSettingsRepository.findByUser(freshUser)
                .orElseGet(() -> createDefaultSettings(freshUser));
        settings.updateSettings(commentEnabled, chatEnabled, null, null, null, null, categoryEnabled);
        NotificationSettings saved = notificationSettingsRepository.save(settings);
        return notificationConverter.toSettingsDTO(saved, freshUser);
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

            // 트랜잭션 커밋 이후 SSE 전송 (실패 시에도 DB 저장은 보장)
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            // SSE로 실시간 푸시
                            sseEmitterService.sendToUser(
                                    user.getId(),
                                    notificationConverter.toListDTO(notification)
                            );
                            log.info("SSE 알림 전송 시도: userId={}, notificationId={}",
                                    user.getId(), notification.getNotificationId());
                        } catch (Exception e) {
                            log.warn("SSE 알림 전송 실패 (SSE 미연결 가능성): userId={}", user.getId(), e);
                        }
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
        int page = 0;
        Page<User> users;
        do {
            users = userRepository.findAllActiveUsers(PageRequest.of(page++, 500));
            for (User user : users) {
                NotificationSettings settings = notificationSettingsRepository.findByUser(user).orElse(null);
                if (settings == null || Boolean.TRUE.equals(settings.getNoticeEnabled())) {
                    createNotification(user, NotificationType.NOTICE, title, message, ReferenceType.NOTICE, noticeId);
                }
            }
        } while (users.hasNext());
    }

    /**
     * 게시글 생성 시 카테고리 기준 알림
     * - 동일 카테고리를 구독한 사용자에게 알림 발송
     */
    @Transactional
    public void notifyCategoriesForPost(Post post) {
        if (post.getCategory() == null) {
            log.debug("게시글에 카테고리가 없어 카테고리 알림을 건너뜁니다. postId={}", post.getId());
            return;
        }
        
        // 해당 카테고리를 구독한 사용자 조회
        var subscribedUsers = userCategoryRepository.findAllByCategory(post.getCategory());
        if (subscribedUsers.isEmpty()) {
            log.debug("카테고리 {}를 구독한 사용자가 없습니다. postId={}", post.getCategory(), post.getId());
            return;
        }
        
        // 게시글 작성자는 제외하고 알림 발송
        for (var userCategory : subscribedUsers) {
            User subscriber = userCategory.getUser();
            
            // 게시글 작성자에게는 알림 발송하지 않음
            if (post.getUser() != null && subscriber.getId().equals(post.getUser().getId())) {
                continue;
            }
            
            // 알림 설정 확인 및 발송
            NotificationSettings settings = notificationSettingsRepository.findByUser(subscriber).orElse(null);
            if (settings != null && !Boolean.TRUE.equals(settings.getCategoryEnabled())) {
                continue; // 카테고리 알림이 꺼져있으면 건너뜀
            }
            
            String categoryName = getCategoryName(post.getCategory());
            String title = "새로운 " + categoryName + " 게시글이 등록되었습니다";
            String message = post.getTitle();
            
            createNotification(
                subscriber,
                NotificationType.CATEGORY,
                title,
                message,
                ReferenceType.POST,
                post.getId()
            );
        }
        
        log.info("카테고리 알림 발송 완료: postId={}, category={}, subscribers={}", 
                post.getId(), post.getCategory(), subscribedUsers.size());
    }
    
    /**
     * 카테고리 이름 반환
     */
    private String getCategoryName(com.fmi.domain.Enum.Category category) {
        return switch (category) {
            case ELECTRONICS -> "전자기기";
            case WALLET -> "지갑";
            case ID_CARD -> "신분증";
            case JEWELRY -> "귀금속";
            case BAG -> "가방";
            case CARD -> "카드";
            case ETC -> "기타";
        };
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
                        // SSE로 실시간 푸시
                        sseEmitterService.sendToUser(
                                notification.getUser().getId(),
                                notificationConverter.toListDTO(notification)
                        );
                        log.info("SSE 알림 갱신 전송: userId={}, notificationId={}",
                                notification.getUser().getId(), notification.getNotificationId());
                    } catch (Exception e) {
                        log.warn("SSE 알림 갱신 전송 실패: userId={}", notification.getUser().getId(), e);
                    }
                }
            });
        }
    }
    
}

