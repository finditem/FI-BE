package com.fmi.domain.post.post.repository;

import com.fmi.domain.post.post.data.Post;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostContentPolicyRepository extends org.springframework.data.repository.Repository<Post, Long> {

    // 콘텐츠 활용 동의 유저 게시글 - 최신순
    @Query(value = """
            SELECT p.* FROM post p
            INNER JOIN users u ON p.user_id = u.id
            WHERE u.content_policy_agreed = true AND u.deleted = false
              AND p.temporary_save = false AND p.deleted = false
              AND (:category IS NULL OR p.category = :category)
              AND (:postStatus IS NULL OR p.item_status = :postStatus)
              AND (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%'))
              AND (:startDate IS NULL OR p.created_at >= :startDate)
              AND (:endDate IS NULL OR p.created_at <= :endDate)
              AND (:cursor IS NULL OR p.id < :cursor)
            ORDER BY p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Post> findContentPolicyPostsByLatest(
            @Param("category") String category,
            @Param("postStatus") String postStatus,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    // 콘텐츠 활용 동의 유저 게시글 - 오래된순
    @Query(value = """
            SELECT p.* FROM post p
            INNER JOIN users u ON p.user_id = u.id
            WHERE u.content_policy_agreed = true AND u.deleted = false
              AND p.temporary_save = false AND p.deleted = false
              AND (:category IS NULL OR p.category = :category)
              AND (:postStatus IS NULL OR p.item_status = :postStatus)
              AND (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%'))
              AND (:startDate IS NULL OR p.created_at >= :startDate)
              AND (:endDate IS NULL OR p.created_at <= :endDate)
              AND (:cursor IS NULL OR p.id > :cursor)
            ORDER BY p.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Post> findContentPolicyPostsByOldest(
            @Param("category") String category,
            @Param("postStatus") String postStatus,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    // 콘텐츠 활용 동의 유저 게시글 - 조회수 많은순
    @Query(value = """
            SELECT p.* FROM post p
            INNER JOIN users u ON p.user_id = u.id
            WHERE u.content_policy_agreed = true AND u.deleted = false
              AND p.temporary_save = false AND p.deleted = false
              AND (:category IS NULL OR p.category = :category)
              AND (:postStatus IS NULL OR p.item_status = :postStatus)
              AND (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%'))
              AND (:startDate IS NULL OR p.created_at >= :startDate)
              AND (:endDate IS NULL OR p.created_at <= :endDate)
              AND (:cursorViewCount IS NULL OR (p.view_count < :cursorViewCount OR (p.view_count = :cursorViewCount AND p.id < :cursorId)))
            ORDER BY p.view_count DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Post> findContentPolicyPostsByMostViewed(
            @Param("category") String category,
            @Param("postStatus") String postStatus,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cursorViewCount") Long cursorViewCount,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);

    // 콘텐츠 활용 동의 유저 게시글 - 즐겨찾기 많은순
    @Query(value = """
            SELECT p.*, COUNT(pf.favorite_id) AS fav_count FROM post p
            INNER JOIN users u ON p.user_id = u.id
            LEFT JOIN post_favorite pf ON pf.post_id = p.id AND pf.is_favorite = true
            WHERE u.content_policy_agreed = true AND u.deleted = false
              AND p.temporary_save = false AND p.deleted = false
              AND (:category IS NULL OR p.category = :category)
              AND (:postStatus IS NULL OR p.item_status = :postStatus)
              AND (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%'))
              AND (:startDate IS NULL OR p.created_at >= :startDate)
              AND (:endDate IS NULL OR p.created_at <= :endDate)
            GROUP BY p.id
            HAVING (:cursorFavCount IS NULL OR COUNT(pf.favorite_id) < :cursorFavCount
                    OR (COUNT(pf.favorite_id) = :cursorFavCount AND p.id < :cursorId))
            ORDER BY fav_count DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Post> findContentPolicyPostsByMostFavorited(
            @Param("category") String category,
            @Param("postStatus") String postStatus,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cursorFavCount") Long cursorFavCount,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);
}
