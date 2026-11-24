package com.fmi.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import com.fmi.domain.notification.data.Notification;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.repository.NotificationRepository;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfigureMockMvc
@DisplayName("NotificationSubscriptionService")
class NotificationSubscriptionServiceTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc http;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryService notificationDeliveryService;

    @Autowired
    private SseEmitterService sseEmitterService;

    @Autowired
    private PlatformTransactionManager transactions;

    private User member;

    @AfterEach
    void tearDown() {
        if (member == null) {
            return;
        }
        sseEmitterService.disconnect(member.getId());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            User saved = userRepository.findById(member.getId()).orElseThrow();
            notificationRepository.deleteByUser(saved);
            userRepository.delete(saved);
        });
    }

    @Nested
    @DisplayName("알림을 구독할 때")
    class DescribeSubscribe {

        @Nested
        @DisplayName("미확인 알림이 있으면")
        class ContextWithUnreadNotifications {

            @Test
            @DisplayName("connect 이벤트만 전송하고 미확인 알림은 재전송하지 않는다")
            void itSendsOnlyConnectEvent() throws Exception {
                // given
                member = userRepository.save(User.builder()
                        .profile_img("")
                        .email("sse-unread@test.com")
                        .nickname("SSE미확인회원")
                        .password("test-password")
                        .role(Role.USER)
                        .build());
                notificationRepository.save(Notification.builder()
                        .user(member)
                        .type(NotificationType.COMMENT)
                        .title("이미 있던 댓글 알림")
                        .message("미확인 상태")
                        .referenceType(ReferenceType.POST)
                        .referenceId(1L)
                        .build());

                // when
                MvcResult result = http.perform(get("/notifications/subscribe").with(user(member.getEmail())))
                        .andExpect(request().asyncStarted())
                        .andReturn();

                // then
                String stream = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                assertThat(stream).contains("event:connect");
                assertThat(stream).doesNotContain("event:notification");
                assertThat(stream).doesNotContain("이미 있던 댓글 알림");
            }
        }

        @Nested
        @DisplayName("구독 중 새 알림이 커밋되면")
        class ContextWithCommittedNewNotification {

            @Test
            @DisplayName("새 알림을 notification 이벤트로 전송한다")
            void itSendsCommittedNotification() throws Exception {
                // given
                member = userRepository.save(User.builder()
                        .profile_img("")
                        .email("sse-new@test.com")
                        .nickname("SSE새알림회원")
                        .password("test-password")
                        .role(Role.USER)
                        .build());
                MvcResult result = http.perform(get("/notifications/subscribe").with(user(member.getEmail())))
                        .andExpect(request().asyncStarted())
                        .andReturn();

                // when
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    User receiver = userRepository.findById(member.getId()).orElseThrow();
                    notificationDeliveryService.createNotification(
                            receiver, NotificationType.COMMENT, "새 댓글 알림", "구독 중 생성", ReferenceType.POST, 1L);
                });

                // then
                String stream = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                assertThat(stream).contains("event:connect");
                assertThat(stream).contains("event:notification");
                assertThat(stream).contains("새 댓글 알림");
            }
        }
    }
}
