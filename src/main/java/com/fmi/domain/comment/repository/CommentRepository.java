package com.fmi.domain.comment.repository;

import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.user.data.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 사용자의 댓글 조회 (익명화 처리용)
    List<Comment> findByUser(User user);

    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.user = :user")
    List<Comment> findAllWithPostByUser(@Param("user") User user);

    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.user = :user ORDER BY c.id DESC")
    Slice<Comment> findByUserOrderByIdDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.user = :user AND c.id < :cursor ORDER BY c.id DESC")
    Slice<Comment> findByUserAndIdLessThanOrderByIdDesc(
            @Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);

    @Query(
            "SELECT c FROM Comment c JOIN FETCH c.post WHERE c.user = :user AND c.deleted = false ORDER BY c.createdAt DESC")
    Slice<Comment> findByUserAndDeletedFalseOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    @Query(
            "SELECT c FROM Comment c JOIN FETCH c.post WHERE c.user = :user AND c.deleted = false AND c.createdAt < :cursor ORDER BY c.createdAt DESC")
    Slice<Comment> findByUserAndDeletedFalseAndCreatedAtBeforeOrderByCreatedAtDesc(
            @Param("user") User user, @Param("cursor") java.time.LocalDateTime cursor, Pageable pageable);

    @Query("""
            SELECT c FROM Comment c JOIN FETCH c.post
            WHERE c.user = :user AND c.deleted = false
              AND (:cursor IS NULL OR c.id < :cursor)
              AND (:startDate IS NULL OR c.createdAt >= :startDate)
              AND (:endDate IS NULL OR c.createdAt < :endDate)
              AND (:keyword IS NULL OR c.content LIKE CONCAT('%', :keyword, '%')
                   OR c.post.title LIKE CONCAT('%', :keyword, '%'))
            ORDER BY c.id DESC
            """)
    Slice<Comment> searchMyCommentsLatest(
            @Param("user") User user,
            @Param("cursor") Long cursor,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT c FROM Comment c JOIN FETCH c.post
            WHERE c.user = :user AND c.deleted = false
              AND (:cursor IS NULL OR c.id > :cursor)
              AND (:startDate IS NULL OR c.createdAt >= :startDate)
              AND (:endDate IS NULL OR c.createdAt < :endDate)
              AND (:keyword IS NULL OR c.content LIKE CONCAT('%', :keyword, '%')
                   OR c.post.title LIKE CONCAT('%', :keyword, '%'))
            ORDER BY c.id ASC
            """)
    Slice<Comment> searchMyCommentsOldest(
            @Param("user") User user,
            @Param("cursor") Long cursor,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword,
            Pageable pageable);

    // 활동 내역용 - 날짜/키워드 필터 포함
    @Query("""
            SELECT c FROM Comment c JOIN FETCH c.post
            WHERE c.user = :user AND c.deleted = false
              AND (:startDate IS NULL OR c.createdAt >= :startDate)
              AND (:endDate IS NULL OR c.createdAt < :endDate)
              AND (:keyword IS NULL OR c.content LIKE CONCAT('%', :keyword, '%')
                   OR c.post.title LIKE CONCAT('%', :keyword, '%'))
              AND (:cursor IS NULL OR c.createdAt < :cursor)
            ORDER BY c.createdAt DESC
            """)
    Slice<Comment> findUserActivityComments(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);

    long countByUser(User user);

    @Query("select c from Comment c where c.post.id = :postId order by c.id desc")
    Slice<Comment> findTopByPostIdOrderByIdDesc(@Param("postId") Long postId, Pageable pageable);

    @Query("select c from Comment c where c.post.id = :postId and c.id < :cursor order by c.id desc")
    Slice<Comment> findByPostIdAndIdLessThanOrderByIdDesc(
            @Param("postId") Long postId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("""
            select c
            from Comment c
            where c.post.id = :postId
              and c.parent is null
              and (
                    :excludedEmpty = true
                    or c.user.id not in :excludedUserIds
                  )
            order by c.id desc
            """)
    List<Comment> findParentComments(
            @Param("postId") Long postId,
            @Param("excludedUserIds") Set<Long> excludedUserIds,
            @Param("excludedEmpty") boolean excludedEmpty,
            Pageable pageable);

    @Query("""
            select count(c)
            from Comment c
            where c.post.id = :postId
              and c.parent is null
              and (
                    :excludedEmpty = true
                    or c.user.id not in :excludedUserIds
                  )
            """)
    long countParentComments(
            @Param("postId") Long postId,
            @Param("excludedUserIds") Set<Long> excludedUserIds,
            @Param("excludedEmpty") boolean excludedEmpty);

    @Query("""
            select c
            from Comment c
            where c.parent.id = :parentId
              and (
                    :excludedEmpty = true
                    or c.user.id not in :excludedUserIds
                  )
            order by c.id asc
            """)
    List<Comment> findReplies(
            @Param("parentId") Long parentId,
            @Param("excludedUserIds") Set<Long> excludedUserIds,
            @Param("excludedEmpty") boolean excludedEmpty,
            Pageable pageable);

    @Query("""
            select count(c)
            from Comment c
            where c.parent.id = :parentId
              and (
                    :excludedEmpty = true
                    or c.user.id not in :excludedUserIds
                  )
            """)
    long countReplies(
            @Param("parentId") Long parentId,
            @Param("excludedUserIds") Set<Long> excludedUserIds,
            @Param("excludedEmpty") boolean excludedEmpty);

    @Query("""
                select c
                from Comment c
                where c.parent.id = :parentId
                  and c.id < :cursor
                order by c.id desc
            """)
    List<Comment> findRepliesWithCursor(
            @Param("parentId") Long parentId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("""
                select c.parent.id, count(c)
                from Comment c
                where c.parent.id in :parentIds
                group by c.parent.id
            """)
    List<Object[]> countRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    @Query("""
                select c.post.id, count(c)
                from Comment c
                where c.post.id in :postIds
                  and c.deleted = false
                group by c.post.id
            """)
    List<Object[]> countCommentsGroupByPostId(@Param("postIds") List<Long> postIds);

    @Query("""
            select count(c)
                    from Comment c
                    where c.post.id = :postId
                      and c.deleted = false
            """)
    Long countByPostId(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("""
                delete from Comment c
                where c.post.id in :postIds
            """)
    void deleteAllByPostIds(@Param("postIds") List<Long> postIds);
}
