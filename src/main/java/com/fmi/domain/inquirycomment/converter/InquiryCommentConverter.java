package com.fmi.domain.inquirycomment.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquirycomment.data.InquiryComment;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import com.fmi.domain.inquirycomment.web.dto.CreateInquiryCommentDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InquiryCommentConverter {

    /**
     * DTO를 엔티티로 변환
     */
    public InquiryComment toCommentEntity(CreateInquiryCommentDto dto, User user, Inquiry inquiry, InquiryComment parent, String email) {
        InquiryComment.InquiryCommentBuilder builder = InquiryComment.builder()
                .inquiry(inquiry)
                .user(user)
                .parent(parent)
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());

        // 비회원인 경우 이메일 설정
        if (user == null && email != null && !email.isBlank()) {
            builder.email(email);
        }

        return builder.build();
    }

    /**
     * 엔티티를 Response로 변환
     * 대댓글 목록은 Service에서 권한 체크 후 변환하므로 여기서는 포함하지 않음
     */
    public InquiryCommentResponse toCommentResponse(InquiryComment comment, boolean canEdit, boolean canDelete) {
        User author = comment.getUser();
        Long authorId = author != null ? author.getId() : null;
        String authorName = author != null ? author.getNickname() : "비회원";
        String authorEmail = comment.getEmail() != null ? comment.getEmail() : (author != null ? author.getEmail() : null);

        return InquiryCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(authorId)
                .authorName(authorName)
                .authorEmail(authorEmail)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replies(null) // 대댓글은 Service에서 처리
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .canEdit(canEdit)
                .canDelete(canDelete)
                .build();
    }
}
