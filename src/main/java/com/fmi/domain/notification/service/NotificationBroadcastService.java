package com.fmi.domain.notification.service;

import com.fmi.domain.notification.data.NotificationSettings;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.repository.NotificationSettingsRepository;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserCategoryRepository;
import com.fmi.domain.user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationBroadcastService {
    private final NotificationDeliveryService notificationDeliveryService;

    private final NotificationSettingsRepository notificationSettingsRepository;

    private final UserRepository userRepository;

    private final UserCategoryRepository userCategoryRepository;

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
                NotificationSettings settings =
                        notificationSettingsRepository.findByUser(user).orElse(null);
                if (settings == null || Boolean.TRUE.equals(settings.getNoticeEnabled())) {
                    notificationDeliveryService.createNotification(
                            user, NotificationType.NOTICE, "새로운 공지사항이 등록되었습니다.", title, ReferenceType.NOTICE, noticeId);
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
            if (post.getUser() != null
                    && subscriber.getId().equals(post.getUser().getId())) {
                continue;
            }

            // 알림 설정 확인 및 발송
            NotificationSettings settings =
                    notificationSettingsRepository.findByUser(subscriber).orElse(null);
            if (settings != null && !Boolean.TRUE.equals(settings.getCategoryEnabled())) {
                continue; // 카테고리 알림이 꺼져있으면 건너뜀
            }

            String categoryName = getCategoryName(post.getCategory());
            String title = categoryName + " 새 게시글이 등록되었어요.";
            String message = post.getTitle();

            notificationDeliveryService.createNotification(
                    subscriber, NotificationType.CATEGORY, title, message, ReferenceType.POST, post.getId());
        }

        log.info(
                "카테고리 알림 발송 완료: postId={}, category={}, subscribers={}",
                post.getId(),
                post.getCategory(),
                subscribedUsers.size());
    }

    /**
     * 카테고리 이름 반환
     */
    private String getCategoryName(com.fmi.domain.post.post.data.enums.Category category) {
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
}
