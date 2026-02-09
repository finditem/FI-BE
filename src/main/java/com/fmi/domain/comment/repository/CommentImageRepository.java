package com.fmi.domain.comment.repository;

import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.data.CommentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentImageRepository extends JpaRepository<CommentImage, Long> {
    List<CommentImage> findByComment_IdIn(List<Long> commentIds);

    List<CommentImage> findByIdInAndComment_Id(List<Long> ids, Long commentId);

    List<CommentImage> findByComment(Comment comment);

    void deleteAllByComment(Comment comment);

}
