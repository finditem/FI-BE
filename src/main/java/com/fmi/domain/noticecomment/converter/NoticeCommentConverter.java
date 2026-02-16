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
        return toResponse(comment, false, false);
    }

    public NoticeCommentResponse toResponse(NoticeComment comment, boolean canEdit, boolean canDelete) {
        User author = comment.getUser();
        Long authorId = author != null ? author.getId() : null;
        String authorName = author != null ? author.getNickname() : null;

        return NoticeCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(authorId)
                .authorName(authorName)
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .canEdit(canEdit)
                .canDelete(canDelete)
                .build();
    }
}
