package com.fmi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Comparator;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    private static final List<String> TAG_ORDER = List.of(
            "Auth", "User", "Post", "Place", "Chat", "Inquiry", "Report", "Block", "Notice", "Notification", "System");

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("FMI API").version("v1.0").description("FMI API 명세서"))
                .components(new Components()
                        .addSecuritySchemes(
                                "BearerToken",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .security(List.of(new SecurityRequirement().addList("BearerToken")))
                .tags(List.of(
                        new Tag().name("Auth").description("회원가입, 로그인, 이메일 인증, 소셜 로그인과 비밀번호 관리를 제공합니다."),
                        new Tag().name("User").description("회원 정보, 활동, 구독 카테고리와 즐겨찾기를 관리합니다."),
                        new Tag().name("Post").description("게시글 작성, 조회와 지도 탐색, 댓글과 좋아요를 제공합니다."),
                        new Tag().name("Place").description("장소 탐색, 장소 좋아요와 운영진 장소 관리를 제공합니다."),
                        new Tag().name("Chat").description("채팅방, 메시지와 메시지 번역을 관리합니다."),
                        new Tag().name("Inquiry").description("회원과 게스트의 문의, 문의 댓글과 운영진 처리를 제공합니다."),
                        new Tag().name("Report").description("회원 신고 접수와 운영진 신고 처리를 제공합니다."),
                        new Tag().name("Block").description("회원 사이의 차단 관계를 관리합니다."),
                        new Tag().name("Notice").description("공지사항 조회, 댓글과 추천, 운영진 관리를 제공합니다."),
                        new Tag().name("Notification").description("서비스 내 알림과 브라우저 알림 구독을 관리합니다."),
                        new Tag().name("System").description("서비스 상태와 파일 저장소 연동을 확인합니다.")));
    }

    @Bean
    public OpenApiCustomizer sortTagsByConfiguredOrder() {
        return openApi -> {
            List<Tag> tags = openApi.getTags();
            if (tags == null || tags.isEmpty()) return;

            tags.sort(Comparator.comparingInt((Tag tag) -> {
                        int index = TAG_ORDER.indexOf(tag.getName());
                        return index >= 0 ? index : Integer.MAX_VALUE;
                    })
                    .thenComparing(Tag::getName, String.CASE_INSENSITIVE_ORDER));
        };
    }
}
