package com.fmi.domain.inquiry.converter;

import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import org.springframework.stereotype.Component;

@Component
public class InquiryConverter {
    
    public InquiryListDTO toListDTO(Inquiry inquiry) {
        return InquiryListDTO.builder()
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .inquiryType(inquiry.getInquiryType())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .answered(inquiry.getAnswered())
                .build();
    }

    public InquiryDetailDTO toDetailDTO(Inquiry inquiry) {
        return InquiryDetailDTO.builder()
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .email(inquiry.getEmail())
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .answered(inquiry.getAnswered())
                .build();
    }

    public InquiryDetailDTO toDetailDTO(Inquiry inquiry, java.util.List<InquiryCommentResponse> comments) {
        return InquiryDetailDTO.builder()
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .email(inquiry.getEmail())
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .answered(inquiry.getAnswered())
                .comments(comments)
                .build();
    }
}

