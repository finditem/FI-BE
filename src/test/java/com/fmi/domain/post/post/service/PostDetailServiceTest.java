package com.fmi.domain.post.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fmi.domain.notification.data.Notification;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.repository.NotificationRepository;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.data.enums.Radius;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("PostDetailService")
class PostDetailServiceTest extends IntegrationTestSupport {
    @Autowired
    private PostDetailService postDetailService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    @Nested
    @DisplayName("게시글 상세를 조회한 뒤 관련 알림을 읽음 처리할 때")
    class DescribeReadNotifications {
        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("알림 처리 실패 여부와 관계없이 먼저 집계된 조회수는 유지한다")
        void itPreservesCommittedViewCountWhenNotificationFails(boolean failNotification) {
            User author = userRepository.save(User.builder()
                    .email("structure-detail-author@test.com")
                    .nickname("구조상세작성자")
                    .password("test-password")
                    .role(Role.USER)
                    .build());
            User viewer = userRepository.save(User.builder()
                    .email("structure-detail-viewer@test.com")
                    .nickname("구조상세조회자")
                    .password("test-password")
                    .role(Role.USER)
                    .build());
            Post post = postRepository.save(Post.create(
                    "검증 지갑",
                    "서울",
                    37.54,
                    127.05,
                    PostType.LOST,
                    Category.WALLET,
                    "상세 조회 트랜잭션 검증",
                    false,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    author));
            Notification notification = notificationRepository.save(Notification.builder()
                    .user(viewer)
                    .type(NotificationType.COMMENT)
                    .title("구조 트랜잭션 검증 알림")
                    .message("새 댓글")
                    .referenceType(ReferenceType.POST)
                    .referenceId(post.getId())
                    .build());
            var principal = org.springframework.security.core.userdetails.User.withUsername(viewer.getEmail())
                    .password("test-password")
                    .roles("USER")
                    .build();
            boolean guardInstalled = false;
            try {
                if (failNotification) {
                    jdbc.execute("ALTER TABLE notification ADD CONSTRAINT structure_fail_notification CHECK ("
                            + "title <> '구조 트랜잭션 검증 알림' OR is_read = false)");
                    guardInstalled = true;
                }
                Throwable failure = catchThrowable(() -> postDetailService.getPost(post.getId(), principal, null));
                if (failNotification) {
                    assertThat(failure).isInstanceOf(DataAccessException.class);
                } else {
                    assertThat(failure).isNull();
                }
                assertThat(postRepository.findById(post.getId()).orElseThrow().getViewCount())
                        .isEqualTo(1L);
                assertThat(notificationRepository
                                .findById(notification.getNotificationId())
                                .orElseThrow()
                                .isRead())
                        .isEqualTo(!failNotification);
            } finally {
                if (guardInstalled) {
                    jdbc.execute("ALTER TABLE notification DROP CHECK structure_fail_notification");
                }
                notificationRepository.deleteById(notification.getNotificationId());
                postRepository.deleteById(post.getId());
                userRepository.deleteAllById(java.util.List.of(author.getId(), viewer.getId()));
                var keys = redis.keys("post:view:" + post.getId() + ":*");
                if (keys != null && !keys.isEmpty()) {
                    redis.delete(keys);
                }
            }
        }
    }
}
