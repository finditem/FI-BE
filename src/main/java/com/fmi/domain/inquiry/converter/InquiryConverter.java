package com.fmi.domain.inquiry.converter;

import com.fmi.domain.admin.web.dto.response.AdminInquiryDetailResponse;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.web.dto.response.InquiryCommentResponse;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailResponse;
import com.fmi.domain.inquiry.web.dto.response.InquiryListResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InquiryConverter {

    public InquiryListResponse toListDTO(Inquiry inquiry) {
        return InquiryListResponse.builder()
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .inquiryType(inquiry.getInquiryType())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }

    public InquiryDetailResponse toDetailDTO(Inquiry inquiry) {
        return InquiryDetailResponse.builder()
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .email(inquiry.getEmail())
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }

    public InquiryDetailResponse toDetailDTO(Inquiry inquiry, List<InquiryCommentResponse> comments) {
        return InquiryDetailResponse.builder()
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

    public InquiryDetailResponse toDetailDTO(
            Inquiry inquiry, List<InquiryCommentResponse> comments, List<String> imageUrls) {
        return InquiryDetailResponse.builder()
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

    public AdminInquiryDetailResponse toAdminDetailDTO(Inquiry inquiry, List<InquiryCommentResponse> comments) {
        return toAdminDetailDTO(inquiry, comments, List.of());
    }

    public AdminInquiryDetailResponse toAdminDetailDTO(
            Inquiry inquiry, List<InquiryCommentResponse> comments, List<String> imageUrls) {
        return AdminInquiryDetailResponse.builder()
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .email(
                        inquiry.getEmail() != null
                                ? inquiry.getEmail()
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
