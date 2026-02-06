package com.fmi.domain.comment.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 사용자의 댓글 조회 (익명화 처리용)
    List<Comment> findByUser(User user);

    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.user = :user")
    List<Comment> findAllWithPostByUser(@Param("user") User user);

    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.user = :user ORDER BY c.id DESC")
    Slice<Comment> findByUserOrderByIdDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.user = :user AND c.id < :cursor ORDER BY c.id DESC")
    Slice<Comment> findByUserAndIdLessThanOrderByIdDesc(@Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);

    long countByUser(User user);

    @Query("select c from Comment c where c.post.id = :postId order by c.id desc")
    Slice<Comment> findTopByPostIdOrderByIdDesc(@Param("postId") Long postId, Pageable pageable);

    @Query("select c from Comment c where c.post.id = :postId and c.id < :cursor order by c.id desc")
    Slice<Comment> findByPostIdAndIdLessThanOrderByIdDesc(@Param("postId") Long postId, @Param("cursor") Long cursor, Pageable pageable);


    @Query("""
                select c
                from Comment c
                where c.post.id = :postId
                    and c.parent is null
                order by c.id desc
            """)
    List<Comment> findParentComments(@Param("postId") Long postId, Pageable pageable);

    @Query("""
                select c
                from Comment c
                where c.post.id = :postId
                  and c.parent is null
                  and c.id < :cursor
                order by c.id desc
            """)
    List<Comment> findParentCommentsWithCursor(@Param("postId") Long postId,
                                               @Param("cursor") Long cursor,
                                               Pageable pageable);

    @Query("""
                select c
                from Comment c
                where c.parent.id = :parentId
                order by c.id desc
            """)
    List<Comment> findReplies(@Param("parentId") Long parentId, Pageable pageable);

    @Query("""
                select c
                from Comment c
                where c.parent.id = :parentId
                  and c.id < :cursor
                order by c.id desc
            """)
    List<Comment> findRepliesWithCursor(@Param("parentId") Long parentId,
                                        @Param("cursor") Long cursor,
                                        Pageable pageable);

    @Query("""
                select c.parent.id, count(c)
                from Comment c
                where c.parent.id in :parentIds
                group by c.parent.id
            """)
    List<Object[]> countRepliesByParentIds(@Param("parentIds") List<Long> parentIds);
}