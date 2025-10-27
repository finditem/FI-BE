package com.fmi.domain.faq.web.dto;

import com.fmi.domain.faq.data.enums.FaqCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqResponseDTO {
    private Long faqId;
    private String question;
    private String answer;
    private FaqCategory category;
    private Integer viewCount;
    private LocalDateTime createdAt;
}

