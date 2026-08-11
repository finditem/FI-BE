package com.fmi.domain.commentlike.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.commentlike.data.CommentLike;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserAndComment(User user, Comment comment);

    @Query("""
                select cl.comment.id, count(cl)
                from CommentLike cl
                where cl.comment.id in :commentIds
                  and cl.isLike = true
                group by cl.comment.id
            """)
    List<Object[]> countLikesByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Query("""
                select cl.comment.id
                from CommentLike cl
                where cl.comment.id in :commentIds
                  and cl.user = :user
                  and cl.isLike = true
            """)
    List<Long> findLikedCommentIds(
            @Param("commentIds") List<Long> commentIds,
            @Param("user") User user
    );

    int countByComment_IdAndIsLikeTrue(Long commentId);

    void deleteAllByComment(Comment comment);

    @Modifying
    @Query("""
                delete from CommentLike cl
                where cl.comment.post.id in :postIds
            """)
    void deleteAllByPostIds(@Param("postIds") List<Long> postIds);

}
