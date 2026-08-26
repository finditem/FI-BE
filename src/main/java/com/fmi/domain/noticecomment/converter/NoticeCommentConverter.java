package com.fmi.domain.noticecomment.converter;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.noticecomment.data.NoticeComment;
import com.fmi.domain.noticecomment.data.NoticeCommentImage;
import com.fmi.domain.noticecomment.response.NoticeCommentImageResponse;
import com.fmi.domain.noticecomment.response.NoticeCommentResponse;
import com.fmi.domain.noticecomment.web.dto.CreateNoticeCommentDto;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.web.dto.response.UserCommentResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

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

    public NoticeCommentResponse toResponse(
            NoticeComment comment,
            boolean canEdit,
            boolean canDelete,
            boolean isLike,
            long childCommentCount,
            List<NoticeCommentImage> images) {
        User author = comment.getUser();

        UserCommentResponse authorResponse = author != null
                ? new UserCommentResponse(author.getId(), author.getNickname(), author.getProfile_img())
                : null;

        List<NoticeCommentImageResponse> imageList = images != null
                ? images.stream()
                        .map(img -> new NoticeCommentImageResponse(img.getId(), img.getImgUrl()))
                        .toList()
                : List.of();

        return NoticeCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .deleted(comment.isDeleted())
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .authorResponse(authorResponse)
                .childCommentCount(childCommentCount)
                .imageList(imageList)
                .likeCount(comment.getLikeCount())
                .isLike(isLike)
                .canEdit(canEdit)
                .canDelete(canDelete)
                .build();
    }
}
