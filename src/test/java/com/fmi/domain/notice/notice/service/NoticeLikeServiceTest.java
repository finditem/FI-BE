package com.fmi.domain.notice.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.domain.notice.notice.data.Notice;
import com.fmi.domain.notice.notice.repository.NoticeRepository;
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
@DisplayName("NoticeLikeService")
class NoticeLikeServiceTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc http;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private NoticeRepository noticeRepository;

    @Nested
    @DisplayName("공지 좋아요 API를 호출할 때")
    class DescribeLike {
        @Test
        @DisplayName("중복 좋아요는 한 번만 집계하고 취소하면 원래 개수로 돌아간다")
        void itKeepsDuplicateLikesIdempotent() throws Exception {
            User member = userRepository.save(User.builder()
                    .profile_img("")
                    .email("structure-notice-like@test.com")
                    .nickname("구조공지회원")
                    .password("test-password")
                    .role(Role.USER)
                    .build());
            Notice notice = noticeRepository.save(Notice.builder()
                    .title("구조 검증 공지")
                    .content("공지 좋아요 검증")
                    .author(member)
                    .build());
            String path = "/notices/" + notice.getNoticeId() + "/like";
            http.perform(post(path).with(user(member.getEmail()))).andExpect(status().isOk());
            http.perform(post(path).with(user(member.getEmail()))).andExpect(status().isOk());
            entityManager.flush();
            entityManager.clear();
            assertThat(noticeRepository
                            .findById(notice.getNoticeId())
                            .orElseThrow()
                            .getLikeCount())
                    .isEqualTo(1);
            http.perform(delete(path).with(user(member.getEmail()))).andExpect(status().isOk());
            entityManager.flush();
            entityManager.clear();
            assertThat(noticeRepository
                            .findById(notice.getNoticeId())
                            .orElseThrow()
                            .getLikeCount())
                    .isZero();
        }
    }
}
