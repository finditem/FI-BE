package com.fmi.domain.commentlike.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.commentlike.data.CommentLike;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommentLikeConverter {

    public CommentLike toTrueEntity(User user, Comment comment) {
        return CommentLike.builder()
                .user(user)
                .comment(comment)
                .isLiked(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
