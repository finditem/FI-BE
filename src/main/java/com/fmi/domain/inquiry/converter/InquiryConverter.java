package com.fmi.domain.inquiry.converter;

import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import org.springframework.stereotype.Component;

@Component
public class InquiryConverter {
    
    public InquiryListDTO toListDTO(Inquiry inquiry) {
        return InquiryListDTO.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .category(inquiry.getCategory())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
    
    public InquiryDetailDTO toDetailDTO(Inquiry inquiry) {
        return InquiryDetailDTO.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .category(inquiry.getCategory())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}

