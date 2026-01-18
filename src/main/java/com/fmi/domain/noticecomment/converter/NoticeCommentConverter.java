package com.fmi.domain.noticecomment.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.noticecomment.data.NoticeComment;
import com.fmi.domain.noticecomment.response.NoticeCommentResponse;
import com.fmi.domain.noticecomment.web.dto.CreateNoticeCommentDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NoticeCommentConverter {

    public NoticeComment toEntity(CreateNoticeCommentDto dto, User user, Notice notice, NoticeComment parent) {
        return NoticeComment.builder()
                .user(user)
                .notice(notice)
                .parent(parent)
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public NoticeCommentResponse toResponse(NoticeComment comment) {
        return NoticeCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getUser().getNickname())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .build();
    }
}
