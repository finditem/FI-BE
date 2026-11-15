package com.fmi.domain.admin.service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
@DisplayName("AdminUserQueryService")
class AdminUserQueryServiceTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc http;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("관리자 회원 상세 API를 호출할 때")
    class DescribeUserDetail {
        @Test
        @DisplayName("관리자에게 해당 회원 정보를 반환하고 일반 회원의 조회는 거부한다")
        void itPreservesAdministratorBoundary() throws Exception {
            User member = userRepository.save(User.builder()
                    .profile_img("")
                    .email("structure-admin-target@test.com")
                    .nickname("구조조회대상")
                    .password("test-password")
                    .role(Role.USER)
                    .build());
            String path = "/admin/users/" + member.getId();
            http.perform(get(path)).andExpect(status().isUnauthorized());
            http.perform(get(path).with(user("structure-member@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
            http.perform(get(path).with(user("structure-admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.userId").value(member.getId()))
                    .andExpect(jsonPath("$.result.email").value(member.getEmail()));
        }
    }
}
