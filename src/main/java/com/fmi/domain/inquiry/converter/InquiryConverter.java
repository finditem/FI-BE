package com.fmi.domain.inquiry.converter;

import com.fmi.domain.admin.dto.AdminInquiryDetailDTO;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import org.springframework.stereotype.Component;

import java.util.List;

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
                .build();
    }

    public InquiryDetailDTO toDetailDTO(Inquiry inquiry, List<InquiryCommentResponse> comments) {
        return InquiryDetailDTO.builder()
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .email(inquiry.getEmail())
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .comments(comments)
                .build();
    }

    public InquiryDetailDTO toDetailDTO(Inquiry inquiry, List<InquiryCommentResponse> comments, List<String> imageUrls) {
        return InquiryDetailDTO.builder()
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .email(inquiry.getEmail())
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .imageUrls(imageUrls)
                .comments(comments)
                .build();
    }

    public AdminInquiryDetailDTO toAdminDetailDTO(Inquiry inquiry, List<InquiryCommentResponse> comments) {
        return toAdminDetailDTO(inquiry, comments, List.of());
    }

    public AdminInquiryDetailDTO toAdminDetailDTO(Inquiry inquiry, List<InquiryCommentResponse> comments, List<String> imageUrls) {
        return AdminInquiryDetailDTO.builder()
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .email(inquiry.getEmail() != null ? inquiry.getEmail()
                        : inquiry.getUser() != null ? inquiry.getUser().getEmail() : null)
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .answered(inquiry.getAnswered())
                .ip(inquiry.getIp())
                .imageUrls(imageUrls)
                .comments(comments)
                .build();
    }
}
