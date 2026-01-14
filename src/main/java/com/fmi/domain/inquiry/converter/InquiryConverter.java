package com.fmi.domain.inquiry.converter;

import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.data.InquiryReply;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryReplyDTO;
import org.springframework.stereotype.Component;

@Component
public class InquiryConverter {
    
    public InquiryListDTO toListDTO(Inquiry inquiry) {
        return InquiryListDTO.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .category(inquiry.getCategory())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
    
    public InquiryDetailDTO toDetailDTO(Inquiry inquiry, InquiryReply reply) {
        InquiryReplyDTO replyDTO = reply != null ? toReplyDTO(reply) : null;
        
        return InquiryDetailDTO.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .category(inquiry.getCategory())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .reply(replyDTO)
                .build();
    }
    
    public InquiryReplyDTO toReplyDTO(InquiryReply reply) {
        return InquiryReplyDTO.builder()
                .replyId(reply.getReplyId())
                .content(reply.getContent())
                .adminName("관리자")  // 추후 관리자 테이블 생성 시 수정
                .createdAt(reply.getCreatedAt())
                .build();
    }
}

