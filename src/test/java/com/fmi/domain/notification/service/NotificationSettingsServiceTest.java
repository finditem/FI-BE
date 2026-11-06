package com.fmi.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.domain.notification.repository.NotificationSettingsRepository;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
@DisplayName("NotificationSettingsService")
class NotificationSettingsServiceTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc http;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private NotificationSettingsRepository settingsRepository;

    @Nested
    @DisplayName("알림 설정 API를 호출할 때")
    class DescribeSettings {
        @Test
        @DisplayName("첫 조회에 기본 설정을 저장하고 변경한 댓글 알림 설정을 유지한다")
        void itCreatesAndUpdatesSettings() throws Exception {
            User member = userRepository.save(User.builder()
                    .email("structure-settings@test.com")
                    .nickname("구조설정회원")
                    .password("test-password")
                    .role(Role.USER)
                    .build());
            http.perform(get("/notifications/settings").with(user(member.getEmail())))
                    .andExpect(status().isOk());
            assertThat(settingsRepository.findByUser(member)).isPresent();
            http.perform(put("/notifications/settings")
                            .with(user(member.getEmail()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"commentEnabled\":false}"))
                    .andExpect(status().isOk());
            entityManager.flush();
            entityManager.clear();
            assertThat(settingsRepository.findByUser(member).orElseThrow().getCommentEnabled())
                    .isFalse();
        }
    }
}
