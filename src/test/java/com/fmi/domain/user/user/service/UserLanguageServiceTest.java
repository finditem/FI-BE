package com.fmi.domain.user.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.LanguageCode;
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
@DisplayName("UserLanguageService")
class UserLanguageServiceTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc http;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("선호 언어 API를 호출할 때")
    class DescribePreferredLanguage {
        @Test
        @DisplayName("회원의 언어를 변경하고 후속 조회에서도 유지한다")
        void itPersistsPreferredLanguage() throws Exception {
            User member = userRepository.save(User.builder()
                    .email("structure-language@test.com")
                    .nickname("구조언어회원")
                    .password("test-password")
                    .role(Role.USER)
                    .build());
            http.perform(patch("/users/me/preferred-language")
                            .with(user(member.getEmail()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"preferredLanguage\":\"EN\"}"))
                    .andExpect(status().isOk());
            entityManager.flush();
            entityManager.clear();
            assertThat(userRepository.findById(member.getId()).orElseThrow().getPreferredLanguage())
                    .isEqualTo(LanguageCode.EN);
            http.perform(get("/users/me/preferred-language").with(user(member.getEmail())))
                    .andExpect(status().isOk());
        }
    }
}
