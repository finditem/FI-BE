package com.fmi.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * 이메일 템플릿 로드 및 변수 치환 서비스
 */
@Service
@Slf4j
public class EmailTemplateService {

    private static final String TEMPLATE_PATH = "templates/emails/";

    /**
     * 템플릿 파일을 로드하고 변수를 치환합니다.
     *
     * @param templateName 템플릿 파일명 (예: "verify-code.html")
     * @param variables    치환할 변수 맵 (예: {"code": "123456", "name": "홍길동"})
     * @return 치환된 HTML 문자열
     */
    public String loadTemplate(String templateName, Map<String, String> variables) {
        try {
            // 템플릿 파일 로드
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH + templateName);
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // 변수 치환
            String result = template;
            if (variables != null) {
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    String placeholder = "{{" + entry.getKey() + "}}";
                    String value = entry.getValue() != null ? entry.getValue() : "";
                    result = result.replace(placeholder, value);
                }
            }

            log.info("[EMAIL TEMPLATE] 로드 완료: {} (변수 {}개 치환)", templateName, variables != null ? variables.size() : 0);
            return result;

        } catch (IOException e) {
            log.error("[EMAIL TEMPLATE] 템플릿 로드 실패: {}", templateName, e);
            throw new RuntimeException("이메일 템플릿을 로드할 수 없습니다: " + templateName, e);
        }
    }

    /**
     * 템플릿 파일 존재 여부 확인
     *
     * @param templateName 템플릿 파일명
     * @return 존재 여부
     */
    public boolean templateExists(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH + templateName);
            return resource.exists();
        } catch (Exception e) {
            return false;
        }
    }
}
