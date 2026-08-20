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
                        new Tag().name("Auth").description("인증/인가 API (회원가입/로그인·리프레시, 소셜 로그인, 이메일/휴대폰 인증, 비밀번호 재설정)"),
                        new Tag().name("Admin").description("관리자 API"),
                        new Tag().name("Inquiry").description("문의 API"),
                        new Tag().name("Notice").description("공지 API"),
                        new Tag().name("Report").description("신고 API"),
                        new Tag().name("Notification").description("알림 API")));
    }

    @Bean
    public OpenApiCustomizer sortTagsAlphabetically() {
        return openApi -> {
            List<Tag> tags = openApi.getTags();
            if (tags == null || tags.isEmpty()) return;

            tags.sort(Comparator.comparingInt((Tag t) -> {
                        String name = t.getName();
                        if ("Auth".equals(name)) return -1;
                        if ("S3".equals(name)) return 1;
                        if ("Health".equals(name)) return 2;
                        return 0;
                    })
                    .thenComparing(Tag::getName, String.CASE_INSENSITIVE_ORDER));
        };
    }
}
