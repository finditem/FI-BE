package com.fmi.domain.comment.repository;

import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.data.CommentImage;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentImageRepository extends JpaRepository<CommentImage, Long> {
    List<CommentImage> findByComment_IdIn(List<Long> commentIds);

    List<CommentImage> findByIdInAndComment_Id(List<Long> ids, Long commentId);

    List<CommentImage> findByComment(Comment comment);

    void deleteAllByComment(Comment comment);

    @Query("""
                select ci.imgUrl
                from CommentImage ci
                where ci.comment.post.id = :postId
            """)
    List<String> findUrlsByPostId(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                delete from CommentImage ci
                where ci.comment.post.id = :postId
            """)
    void deleteAllByPostId(@Param("postId") Long postId);

}
