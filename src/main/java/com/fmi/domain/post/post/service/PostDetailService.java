package com.fmi.domain.post.post.service;

import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.post.web.dto.response.PostGetResponse;
import com.fmi.domain.user.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostDetailService {
    private final PostQueryService postQueryService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public PostGetResponse getPost(Long postId, UserDetails userDetails, String clientIp) {
        PostGetResponse response = postQueryService.getPost(postId, userDetails, clientIp);

        // 게시글 조회 시 관련 알림 자동 읽음처리
        if (userDetails != null) {
            userRepository
                    .findByEmail(userDetails.getUsername())
                    .ifPresent(user -> notificationService.markNotificationsAsReadByReference(
                            user,
                            postId,
                            List.of(NotificationType.COMMENT, NotificationType.FAVORITE, NotificationType.CATEGORY)));
        }

        return response;
    }
}
