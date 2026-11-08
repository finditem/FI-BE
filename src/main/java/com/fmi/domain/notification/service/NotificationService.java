package com.fmi.domain.notification.service;

import com.fmi.domain.notification.converter.NotificationConverter;
import com.fmi.domain.notification.data.Notification;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.repository.NotificationRepository;
import com.fmi.domain.notification.web.dto.response.NotificationListResponse;
import com.fmi.domain.user.user.data.User;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final NotificationConverter notificationConverter;

    /**
     * 내 알림 목록 조회
     */
    public Page<NotificationListResponse> getMyNotifications(
            User user, Boolean unreadOnly, NotificationType notificationType, Pageable pageable) {
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
    public CursorPageResponse<NotificationListResponse> getMyNotificationsCursor(
            User user, Boolean unreadOnly, NotificationType notificationType, Long cursor, int size) {
        int fetchSize = size + 1;
        Pageable limit = PageRequest.of(0, fetchSize);
        List<Notification> notifications;

        if (notificationType != null && Boolean.TRUE.equals(unreadOnly)) {
            notifications =
                    notificationRepository.findByUserAndTypeAndIsReadFalseCursor(user, notificationType, cursor, limit);
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

        List<NotificationListResponse> responseList =
                content.stream().map(notificationConverter::toListDTO).toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }

    /**
     * 읽지 않은 알림 개수
     */
    public Long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * 읽지 않은 알림 DTO 목록 조회 (SSE 재연결 시 전송용, 최대 50건)
     */
    @Transactional(readOnly = true)
    public List<NotificationListResponse> getUnreadNotificationDTOs(User user) {
        Pageable limit = PageRequest.of(0, 50);
        List<Notification> unread = notificationRepository.findByUserAndIsReadFalseCursor(user, null, limit);
        return unread.stream().map(notificationConverter::toListDTO).toList();
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository
                .findById(notificationId)
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
        Notification notification = notificationRepository
                .findById(notificationId)
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
     * referenceId + 알림 타입 목록으로 읽음 처리
     */
    @Transactional
    public void markNotificationsAsReadByReference(User user, Long referenceId, List<NotificationType> types) {
        List<Notification> notifications =
                notificationRepository.findAllByUserAndReferenceIdAndTypeIn(user, referenceId, types);
        for (Notification n : notifications) {
            if (!n.isRead()) {
                n.markAsRead();
            }
        }
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
}
