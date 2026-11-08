package com.fmi.domain.post.post.web.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@DisplayName("PostOpenApi")
class PostOpenApiTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("실행 중인 애플리케이션의 OpenAPI를 조회할 때")
    class DescribeGetOpenApi {

        @Test
        @DisplayName("분리된 임시저장과 번역 API의 경로, 메서드, Post 태그와 operationId를 유지한다")
        void itPreservesTemporaryPostAndTranslationContracts() throws Exception {
            // given: 전체 애플리케이션이 시작되므로 중복된 Spring MVC 매핑은 context 로딩 시 거절된다.

            // when
            var response = mockMvc.perform(get("/v3/api-docs"));

            // then
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.paths['/posts/temp'].length()").value(3))
                    .andExpect(jsonPath("$.paths['/posts/temp'].get.tags", contains("Post")))
                    .andExpect(
                            jsonPath("$.paths['/posts/temp'].get.operationId").value("getTemporaryPost"))
                    .andExpect(jsonPath("$.paths['/posts/temp'].post.tags", contains("Post")))
                    .andExpect(
                            jsonPath("$.paths['/posts/temp'].post.operationId").value("saveTemporaryPost"))
                    .andExpect(jsonPath("$.paths['/posts/temp'].delete.tags", contains("Post")))
                    .andExpect(jsonPath("$.paths['/posts/temp'].delete.operationId")
                            .value("deleteTemporaryPost"))
                    .andExpect(jsonPath("$.paths['/posts/{postId}/translate'].length()")
                            .value(1))
                    .andExpect(jsonPath("$.paths['/posts/{postId}/translate'].get.tags", contains("Post")))
                    .andExpect(jsonPath("$.paths['/posts/{postId}/translate'].get.operationId")
                            .value("translatePost"));
        }
    }
}
