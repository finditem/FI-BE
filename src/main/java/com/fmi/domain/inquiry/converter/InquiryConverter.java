package com.fmi.domain.inquiry.converter;

import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.data.InquiryReply;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryReplyDTO;
import org.springframework.stereotype.Component;

@Component
public class InquiryConverter {
    
    public InquiryListDTO toListDTO(Inquiry inquiry, boolean hasReply) {
        String authorNickname = inquiry.getUser() != null 
                ? inquiry.getUser().getNickname() 
                : "익명";
        
        return InquiryListDTO.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .inquiryType(inquiry.getInquiryType())
                .category(inquiry.getCategory())
                .status(inquiry.getAnswerStatus())
                .authorNickname(authorNickname)
                .hasReply(hasReply)
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
    
    public InquiryDetailDTO toDetailDTO(Inquiry inquiry, InquiryReply reply) {
        String authorNickname = inquiry.getUser() != null 
                ? inquiry.getUser().getNickname() 
                : "익명";
        
        InquiryReplyDTO replyDTO = reply != null ? toReplyDTO(reply) : null;
        
        return InquiryDetailDTO.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .inquiryType(inquiry.getInquiryType())
                .category(inquiry.getCategory())
                .status(inquiry.getAnswerStatus())
                .authorNickname(authorNickname)
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
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

