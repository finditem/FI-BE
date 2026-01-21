package com.fmi.domain.comment.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.response.CommentResponse;
import com.fmi.domain.comment.web.dto.CreateCommentDto;
import com.fmi.domain.post.data.Post;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommentConverter {

    public Comment toCommentEntity(CreateCommentDto dto, User user, Post post, Comment parent) {
        return Comment.builder()
                .user(user)
                .post(post)
                .parent(parent)
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public CommentResponse toCommentResponse(Comment comment) {
        return toCommentResponse(comment, false, false);
    }

    public CommentResponse toCommentResponse(Comment comment, boolean canEdit, boolean canDelete) {
        User author = comment.getUser();
        Long authorId = author != null ? author.getId() : null;
        String authorName = author != null ? author.getNickname() : null;

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(authorId)
                .authorName(authorName)
                .createdAt(comment.getCreatedAt())
                .likeCount(comment.getLikeCount())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .canEdit(canEdit)
                .canDelete(canDelete)
                .build();
    }

}
