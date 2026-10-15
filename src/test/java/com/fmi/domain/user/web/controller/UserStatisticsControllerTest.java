package com.fmi.domain.user.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.support.IntegrationTestSupport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@DisplayName("통계 API 권한")
class UserStatisticsControllerTest extends IntegrationTestSupport {
    @Autowired
    MockMvc http;

    @Nested
    @DisplayName("관리자 통계 API를 요청할 때")
    class DescribeGet {
        @ParameterizedTest
        @ValueSource(strings = {"/admin/users/statistics", "/admin/posts/statistics"})
        @DisplayName("인증하지 않은 요청은 401을 반환한다")
        void itRejectsAnonymous(String path) throws Exception {
            http.perform(get(path).param("startDate", "2026-09-10").param("endDate", "2026-09-10"))
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/admin/users/statistics", "/admin/posts/statistics"})
        @WithMockUser(roles = "USER")
        @DisplayName("일반 회원 요청은 403을 반환한다")
        void itRejectsMember(String path) throws Exception {
            http.perform(get(path).param("startDate", "2026-09-10").param("endDate", "2026-09-10"))
                    .andExpect(status().isForbidden());
        }
    }
}
