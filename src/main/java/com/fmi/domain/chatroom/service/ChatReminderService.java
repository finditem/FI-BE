package com.fmi.domain.chatroom.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatmessage.service.ChatNotificationService;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatReminderService {

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatNotificationService chatNotificationService;

    // 1시간마다 정각에 실행
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void send24HourReminders() {
        log.info("[Scheduler] 24시간 미열람 리마인더 체크 시작");

        // 현재 시간으로부터 24시간 전
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        List<ChatRoomParticipant> targets = chatRoomParticipantRepository.findParticipantsForReminder(threshold);

        if (targets.isEmpty()) {
            return;
        }

        int count = 0;

        for (ChatRoomParticipant pt : targets) {
            try {
                User receiver = pt.getUser();
                Long postId = pt.getChatRoom().getPost().getId();
                String content = pt.getLastMessage().getContent();

                chatNotificationService.saveOrUpdateChatNotification(
                        receiver,
                        postId,
                        content,
                        NotificationType.CHAT_REMINDER
                );

                pt.markReminded();
                count++;

            } catch (Exception e) {
                log.error("리마인더 발송 실패 (User: {}): {}", pt.getUser().getId(), e.getMessage());
            }
        }

        log.info("[Scheduler] 총 {}명에게 리마인더 발송 완료", count);
    }
}
