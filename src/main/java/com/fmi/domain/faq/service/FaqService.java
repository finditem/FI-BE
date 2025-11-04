package com.fmi.domain.faq.service;

import com.fmi.domain.faq.converter.FaqConverter;
import com.fmi.domain.faq.data.Faq;
import com.fmi.domain.faq.data.enums.FaqCategory;
import com.fmi.domain.faq.repository.FaqRepository;
import com.fmi.domain.faq.web.dto.FaqListDTO;
import com.fmi.domain.faq.web.dto.FaqResponseDTO;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService {
    
    private final FaqRepository faqRepository;
    private final FaqConverter faqConverter;
    
    /**
     * FAQ 목록 조회
     */
    public Page<FaqListDTO> getFaqList(FaqCategory category, Pageable pageable) {
        Page<Faq> faqs;
        
        if (category != null) {
            faqs = faqRepository.findByCategory(category, pageable);
        } else {
            faqs = faqRepository.findAll(pageable);
        }
        
        return faqs.map(faqConverter::toListDTO);
    }
    
    /**
     * FAQ 상세 조회
     */
    @Transactional
    public FaqResponseDTO getFaqDetail(Long faqId) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.FAQ_NOT_FOUND));
        
        // 조회수 증가
        faq.increaseViewCount();
        
        return faqConverter.toResponseDTO(faq);
    }
}

