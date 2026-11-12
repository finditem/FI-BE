package com.fmi.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.domain.notification.data.PushSubscription;
import com.fmi.domain.notification.repository.PushSubscriptionRepository;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
@DisplayName("PushSubscriptionService")
class PushSubscriptionServiceTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc http;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @Nested
    @DisplayName("푸시 구독을 등록하고 해제할 때")
    class DescribeSubscribe {
        @Test
        @DisplayName("중복 등록은 기존 키를 유지하고 해제는 요청 회원의 구독만 삭제한다")
        void itPreservesDuplicateAndDeletesOnlyOwnedSubscription() throws Exception {
            User owner = userRepository.save(User.builder()
                    .profile_img("")
                    .email("push-owner@test.com")
                    .nickname("구독자")
                    .role(Role.USER)
                    .password("test-password")
                    .build());
            User other = userRepository.save(User.builder()
                    .profile_img("")
                    .email("push-other@test.com")
                    .nickname("다른 구독자")
                    .role(Role.USER)
                    .password("test-password")
                    .build());
            String endpoint = "https://push.test/subscription";
            subscriptionRepository.save(PushSubscription.builder()
                    .user(other)
                    .endpoint(endpoint)
                    .p256dh("other-key")
                    .auth("other-auth")
                    .build());

            http.perform(post("/push/subscribe")
                            .with(user(owner.getEmail()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                            {"endpoint":"https://push.test/subscription","p256dh":"first-key","auth":"first-auth"}
                            """))
                    .andExpect(status().isOk());
            http.perform(post("/push/subscribe")
                            .with(user(owner.getEmail()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                            {"endpoint":"https://push.test/subscription","p256dh":"replacement","auth":"replacement"}
                            """))
                    .andExpect(status().isOk());
            assertThat(subscriptionRepository.findByUser(owner)).singleElement().satisfies(subscription -> {
                assertThat(subscription.getP256dh()).isEqualTo("first-key");
                assertThat(subscription.getAuth()).isEqualTo("first-auth");
            });

            http.perform(delete("/push/subscribe").with(user(owner.getEmail())).param("endpoint", endpoint))
                    .andExpect(status().isOk());
            assertThat(subscriptionRepository.findByUser(owner)).isEmpty();
            assertThat(subscriptionRepository.findByUser(other)).hasSize(1);
        }
    }
}
