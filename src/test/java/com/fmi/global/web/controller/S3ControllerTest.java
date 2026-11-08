package com.fmi.global.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@DisplayName("S3Controller")
class S3ControllerTest extends IntegrationTestSupport {

    private static final String DELETE_BODY = """
            {"imageUrls": ["https://test-bucket.s3.ap-northeast-2.amazonaws.com/test-image.jpg"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("이미지를 업로드할 때")
    class DescribeUpload {

        @Nested
        @DisplayName("로그인하지 않은 사용자이면")
        class ContextWithoutAuthentication {

            @Test
            @DisplayName("401로 거절한다")
            void itReturnsUnauthorized() throws Exception {
                // given
                MockMultipartFile image =
                        new MockMultipartFile("image", "test-image.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1});

                // when & then
                mockMvc.perform(multipart("/s3/upload").file(image)).andExpect(status().isUnauthorized());
            }
        }

        @Nested
        @DisplayName("일반 회원이면")
        class ContextWithUser {

            @Test
            @WithMockUser(roles = "USER")
            @DisplayName("403으로 거절한다")
            void itReturnsForbidden() throws Exception {
                // given
                MockMultipartFile image =
                        new MockMultipartFile("image", "test-image.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1});

                // when & then
                mockMvc.perform(multipart("/s3/upload").file(image)).andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("관리자이면")
        class ContextWithAdmin {

            @Test
            @WithMockUser(roles = "ADMIN")
            @DisplayName("업로드 요청을 허용한다")
            void itAllowsUpload() throws Exception {
                // given
                MockMultipartFile image =
                        new MockMultipartFile("image", "test-image.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1});

                // when & then
                mockMvc.perform(multipart("/s3/upload").file(image)).andExpect(status().isOk());
            }
        }
    }

    @Nested
    @DisplayName("이미지를 삭제할 때")
    class DescribeDelete {

        @Nested
        @DisplayName("로그인하지 않은 사용자이면")
        class ContextWithoutAuthentication {

            @Test
            @DisplayName("401로 거절한다")
            void itReturnsUnauthorized() throws Exception {
                // when & then
                mockMvc.perform(delete("/s3/delete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(DELETE_BODY))
                        .andExpect(status().isUnauthorized());
            }
        }

        @Nested
        @DisplayName("일반 회원이면")
        class ContextWithUser {

            @Test
            @WithMockUser(roles = "USER")
            @DisplayName("403으로 거절한다")
            void itReturnsForbidden() throws Exception {
                // when & then
                mockMvc.perform(delete("/s3/delete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(DELETE_BODY))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("관리자이면")
        class ContextWithAdmin {

            @Test
            @WithMockUser(roles = "ADMIN")
            @DisplayName("삭제 요청을 허용한다")
            void itAllowsDelete() throws Exception {
                // when & then
                mockMvc.perform(delete("/s3/delete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(DELETE_BODY))
                        .andExpect(status().isOk());
            }
        }
    }
}
