package com.fmi.domain.post.post.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PostController.class, TemporaryPostController.class, PostTranslationController.class})
@Import({SecurityConfig.class, AuthCookieResolver.class})
@ActiveProfiles("test")
@DisplayName("TemporaryPostController")
class TemporaryPostControllerTest {
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
    @DisplayName("임시저장 API를 호출할 때")
    class DescribeTemporaryPost {

        @Nested
        @DisplayName("로그인하지 않은 사용자이면")
        class ContextWithoutAuthentication {

            @Test
            @DisplayName("임시저장 생성은 401을 반환하고 저장하지 않는다")
            void itRejectsAnonymousSave() throws Exception {
                // given
                MockMultipartFile request = new MockMultipartFile("request", "", "application/json", "{}".getBytes());
                // when & then
                mockMvc.perform(multipart("/posts/temp").file(request)).andExpect(status().isUnauthorized());
                verifyNoInteractions(temporaryPostService);
            }

            @Test
            @DisplayName("임시저장 삭제는 401을 반환하고 삭제하지 않는다")
            void itRejectsAnonymousDelete() throws Exception {
                // when & then
                mockMvc.perform(delete("/posts/temp")).andExpect(status().isUnauthorized());
                verifyNoInteractions(temporaryPostService);
            }

            @Test
            @DisplayName("기존 공개 GET 규칙에 따라 인증 정보 없이 조회 서비스에 전달한다")
            void itPreservesPublicGetSecurityRule() throws Exception {
                // when & then: 서비스는 대체하므로 익명 사용자의 실제 조회 성공을 의미하지 않는다.
                mockMvc.perform(get("/posts/temp")).andExpect(status().isOk());
                verify(temporaryPostService).getMyTemporaryPost(null);
            }
        }

        @Nested
        @WithMockUser(username = "test-viewer@example.com")
        @DisplayName("로그인한 사용자이면")
        class ContextWithAuthentication {

            @Test
            @DisplayName("인증된 사용자 정보를 임시저장 조회에 전달한다")
            void itPassesAuthenticatedViewer() throws Exception {
                // when & then
                mockMvc.perform(get("/posts/temp")).andExpect(status().isOk());
                verify(temporaryPostService)
                        .getMyTemporaryPost(argThat(principal ->
                                principal != null && principal.getUsername().equals("test-viewer@example.com")));
            }

            @Test
            @DisplayName("인증된 사용자 정보를 임시저장 생성에 전달한다")
            void itAllowsAuthenticatedSave() throws Exception {
                // given
                MockMultipartFile request = new MockMultipartFile("request", "", "application/json", "{}".getBytes());
                // when & then
                mockMvc.perform(multipart("/posts/temp").file(request)).andExpect(status().isOk());
                verify(temporaryPostService)
                        .create(
                                any(),
                                argThat(principal -> principal != null
                                        && principal.getUsername().equals("test-viewer@example.com")),
                                isNull());
            }

            @Test
            @DisplayName("인증된 사용자 정보를 임시저장 삭제에 전달한다")
            void itAllowsAuthenticatedDelete() throws Exception {
                // when & then
                mockMvc.perform(delete("/posts/temp")).andExpect(status().isOk());
                verify(temporaryPostService)
                        .deleteTemporaryPost(argThat(principal ->
                                principal != null && principal.getUsername().equals("test-viewer@example.com")));
            }
        }
    }
}
