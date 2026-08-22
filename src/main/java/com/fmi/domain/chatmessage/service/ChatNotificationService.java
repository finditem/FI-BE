package com.fmi.domain.chatmessage.service;

import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.notification.data.Notification;
import com.fmi.domain.notification.data.NotificationSettings;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.repository.NotificationRepository;
import com.fmi.domain.notification.repository.NotificationSettingsRepository;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatNotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final NotificationService notificationService;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    /**
     * 채팅 알림을 갱신하거나 새로 생성합니다.
     * 게시글 ID를 referenceId로, 채팅방 ID를 roomId로 사용합니다.
     */
    public void saveOrUpdateChatNotification(
            User receiver, Long postId, Long roomId, String content, NotificationType type) {

        Optional<NotificationSettings> notificationSettings = notificationSettingsRepository.findByUser(receiver);

        if (notificationSettings.isPresent() && !notificationSettings.get().getChatEnabled()) {
            return;
        }

        String title = (type == NotificationType.CHAT_REMINDER) ? "확인하지 않은 채팅이 있어요." : "새로운 채팅이 도착했어요.";

        Optional<Notification> notification =
                notificationRepository.findByUserAndReferenceIdAndRoomIdAndType(receiver, postId, roomId, type);

        if (notification.isPresent()) {
            notificationService.updateChatNotification(notification.get(), content);
        } else {
            notificationService.createNotification(receiver, type, title, content, ReferenceType.CHAT, postId, roomId);
        }
    }

    public void markAsReadOnEnter(Long roomId, Long userId) {
        chatRoomRepository.findById(roomId).ifPresent(chatRoom -> {
            Long postId = chatRoom.isSourcePostDeleted() || chatRoom.getPost() == null
                    ? chatRoom.getSourcePostId()
                    : chatRoom.getPost().getId();
            userRepository
                    .findById(userId)
                    .ifPresent(user -> notificationService.markNotificationsAsReadByReference(
                            user, postId, List.of(NotificationType.CHAT, NotificationType.CHAT_REMINDER)));
        });
    }
}
