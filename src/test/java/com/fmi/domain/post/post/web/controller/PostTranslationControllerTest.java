package com.fmi.domain.post.post.web.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.domain.auth.security.CustomUserDetailsService;
import com.fmi.domain.auth.security.config.SecurityConfig;
import com.fmi.domain.auth.security.cookie.AuthCookieResolver;
import com.fmi.domain.auth.security.jwt.JwtTokenProvider;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.post.service.*;
import com.fmi.domain.user.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PostController.class, TemporaryPostController.class, PostTranslationController.class})
@Import({SecurityConfig.class, AuthCookieResolver.class})
@ActiveProfiles("test")
@DisplayName("PostTranslationController")
class PostTranslationControllerTest {
    @MockitoBean
    private PostDetailService postDetailService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private PostQueryService postQueryService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TemporaryPostService temporaryPostService;

    @MockitoBean
    private PostTranslationService postTranslationService;

    @Nested
    @DisplayName("게시글 번역 API를 호출할 때")
    class DescribeTranslatePost {

        @Test
        @DisplayName("로그인하지 않은 사용자도 인증 정보 없이 번역 서비스에 전달한다")
        void itAllowsAnonymousTranslation() throws Exception {
            // when & then
            mockMvc.perform(get("/posts/13/translate")).andExpect(status().isOk());
            verify(postTranslationService).getOrTranslate(13L, null);
        }

        @Test
        @WithMockUser(username = "test-viewer@example.com")
        @DisplayName("로그인한 사용자의 인증 정보를 번역 서비스에 전달한다")
        void itPassesAuthenticatedViewer() throws Exception {
            // when & then
            mockMvc.perform(get("/posts/13/translate")).andExpect(status().isOk());
            verify(postTranslationService)
                    .getOrTranslate(
                            eq(13L),
                            argThat(principal ->
                                    principal != null && principal.getUsername().equals("test-viewer@example.com")));
        }
    }
}
