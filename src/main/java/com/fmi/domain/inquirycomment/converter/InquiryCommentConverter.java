package com.fmi.domain.inquirycomment.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquirycomment.data.InquiryComment;
import com.fmi.domain.inquirycomment.data.InquiryCommentImage;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import com.fmi.domain.inquirycomment.web.dto.CreateInquiryCommentDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
     */
    public InquiryCommentResponse toCommentResponse(InquiryComment comment, boolean canEdit, boolean canDelete,
                                                     boolean isAdmin, List<InquiryCommentImage> images) {
        User author = comment.getUser();
        Long authorId = author != null ? author.getId() : null;
        String authorName = author != null ? author.getNickname() : "비회원";
        String authorEmail = author != null ? author.getEmail() : comment.getEmail();
        String profileImg = author != null ? author.getProfile_img() : null;
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;

        List<String> imageList = images != null
                ? images.stream().map(InquiryCommentImage::getImgUrl).toList()
                : List.of();

        return InquiryCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(authorId)
                .authorName(authorName)
                .authorEmail(authorEmail)
                .profileImg(profileImg)
                .isAdmin(isAdmin)
                .parentId(parentId)
                .replies(List.of())
                .imageList(imageList)
                .canEdit(canEdit)
                .canDelete(canDelete)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
