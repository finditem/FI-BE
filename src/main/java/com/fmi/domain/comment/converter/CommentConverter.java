package com.fmi.domain.comment.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.response.CommentResponse;
import com.fmi.domain.comment.web.dto.CreateCommentDto;
import com.fmi.domain.comment.web.dto.NotificationDto;
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
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getNickname(),
                comment.getCreatedAt(),
                comment.getLikeCount()
        );
    }

}
