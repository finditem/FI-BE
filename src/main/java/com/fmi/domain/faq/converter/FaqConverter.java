package com.fmi.domain.faq.converter;

import com.fmi.domain.faq.data.Faq;
import com.fmi.domain.faq.web.dto.FaqListDTO;
import com.fmi.domain.faq.web.dto.FaqResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class FaqConverter {
    
    public FaqListDTO toListDTO(Faq faq) {
        String answerPreview = faq.getAnswer().length() > 100 
                ? faq.getAnswer().substring(0, 100) + "..." 
                : faq.getAnswer();
        
        return FaqListDTO.builder()
                .faqId(faq.getFaqId())
                .question(faq.getQuestion())
                .answerPreview(answerPreview)
                .category(faq.getCategory())
                .viewCount(faq.getViewCount())
                .build();
    }
    
    public FaqResponseDTO toResponseDTO(Faq faq) {
        return FaqResponseDTO.builder()
                .faqId(faq.getFaqId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .category(faq.getCategory())
                .viewCount(faq.getViewCount())
                .createdAt(faq.getCreatedAt())
                .build();
    }
}

