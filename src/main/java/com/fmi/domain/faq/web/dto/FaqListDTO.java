package com.fmi.domain.faq.web.dto;

import com.fmi.domain.faq.data.enums.FaqCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqListDTO {
    private Long faqId;
    private String question;
    private String answerPreview;    // 답변 미리보기 (100자)
    private FaqCategory category;
    private Integer viewCount;
}

