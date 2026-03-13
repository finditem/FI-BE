package com.fmi.domain.chatmessage.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notification.data.Notification;
import com.fmi.domain.notification.data.NotificationSettings;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.repository.NotificationRepository;
import com.fmi.domain.notification.repository.NotificationSettingsRepository;
import com.fmi.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatNotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final NotificationService notificationService;

    /**
     * 채팅 알림을 갱신하거나 새로 생성합니다.
     * 게시글 ID를 referenceId로 사용합니다.
     */
    public void saveOrUpdateChatNotification(User receiver, Long postId, String content, NotificationType type) {

        Optional<NotificationSettings> notificationSettings = notificationSettingsRepository.findByUser(receiver);

        if (notificationSettings.isPresent() && !notificationSettings.get().getChatEnabled()) {
            return;
        }

        String title = (type == NotificationType.CHAT_REMINDER)
                ? "확인하지 않은 채팅이 있어요."
                : "새로운 채팅이 도착했어요.";

        Optional<Notification> notification =
                notificationRepository.findByUserAndReferenceIdAndType(
                        receiver, postId, type
                );

        if (notification.isPresent()) {
            notificationService.updateChatNotification(
                    notification.get(),
                    content
            );
        } else {
            notificationService.createNotification(
                    receiver,
                    type,
                    title,
                    content,
                    ReferenceType.CHAT,
                    postId
            );
        }
    }

    /**
     * 채팅방 입장 시 해당 게시글의 CHAT/CHAT_REMINDER 알림을 읽음 처리합니다.
     */
    public void markChatNotificationsAsRead(User user, Long postId) {
        List<Notification> notifications = notificationRepository.findAllByUserAndReferenceIdAndTypeIn(
                user, postId, List.of(NotificationType.CHAT, NotificationType.CHAT_REMINDER)
        );
        for (Notification n : notifications) {
            if (!n.isRead()) {
                n.markAsRead();
            }
        }
    }

}
