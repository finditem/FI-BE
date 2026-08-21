package com.fmi.domain.comment.converter;

import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.web.dto.response.CommentCreateResponse;
import com.fmi.domain.comment.web.dto.response.CommentDeleteResponse;
import com.fmi.domain.comment.web.dto.response.CommentImageResponse;
import com.fmi.domain.comment.web.dto.response.CommentResponse;
import com.fmi.domain.user.web.dto.response.UserCommentResponse;
import java.util.List;

public class CommentConverter {

    public static CommentCreateResponse toCommentCreateResponse(
            Comment comment,
            int likeCount,
            boolean isAuthor,
            UserCommentResponse userCommentResponse,
            List<CommentImageResponse> commentImageResponseList) {
        return new CommentCreateResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                likeCount,
                isAuthor && !comment.isDeleted(),
                isAuthor && !comment.isDeleted(),
                userCommentResponse,
                commentImageResponseList);
    }

    public static CommentResponse toCommentResponse(
            Comment comment,
            UserCommentResponse authorResponse,
            List<CommentImageResponse> imageList,
            long childCommentCount,
            long likeCount,
            boolean isLike,
            boolean isAuthor) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.isDeleted(),
                comment.getDepth(),
                comment.getCreatedAt(),
                authorResponse,
                childCommentCount,
                imageList,
                likeCount,
                isLike,
                isAuthor && !comment.isDeleted(),
                isAuthor && !comment.isDeleted());
    }

    public static CommentDeleteResponse toDeleteResponse(Comment comment) {
        return new CommentDeleteResponse(comment.getId(), comment.getContent());
    }

    //    public Comment toCommentEntity(CreateCommentDto dto, User user, Post post, Comment parent) {
    //        return Comment.builder()
    //                .user(user)
    //                .post(post)
    //                .parent(parent)
    //                .content(dto.getContent())
    //                .createdAt(LocalDateTime.now())
    //                .updatedAt(LocalDateTime.now())
    //                .build();
    //    }
    //
    //    public CommentCreateResponse toCommentResponse(Comment comment) {
    //        return toCommentResponse(comment, false, false);
    //    }
    //
    //    public CommentCreateResponse toCommentResponse(Comment comment, boolean canEdit, boolean canDelete) {
    //        User author = comment.getUser();
    //        Long authorId = author != null ? author.getId() : null;
    //        String authorName = author != null ? author.getNickname() : null;
    //
    //        return CommentCreateResponse.builder()
    //                .id(comment.getId())
    //                .content(comment.getContent())
    //                .authorId(authorId)
    //                .authorName(authorName)
    //                .createdAt(comment.getCreatedAt())
    //                .likeCount(comment.getLikeCount())
    //                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
    //                .canEdit(canEdit)
    //                .canDelete(canDelete)
    //                .build();
    //    }

}
