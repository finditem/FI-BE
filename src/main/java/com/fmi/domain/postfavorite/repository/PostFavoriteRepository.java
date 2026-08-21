package com.fmi.domain.postfavorite.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.postfavorite.data.PostFavorite;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {
    Optional<PostFavorite> findByUserAndPost(User user, Post post);

    List<PostFavorite> findByUserAndIsFavoriteTrue(User user);

    @Query("""
                SELECT pf.user
                FROM PostFavorite pf
                WHERE pf.post = :post
                  AND pf.isFavorite = true
                  AND pf.post.deleted = false
            """)
    List<User> findUsersByPost(@Param("post") Post post);

    @Query(
            "SELECT pf.post.id FROM PostFavorite pf WHERE pf.user = :user AND pf.post IN :posts AND pf.isFavorite = true")
    Set<Long> findPostIdsByUserAndPostIn(@Param("user") User user, @Param("posts") List<Post> posts);

    @Query("""
                select count(pf)
                from PostFavorite pf
                where pf.post = :post
                  and pf.isFavorite = true
                  and pf.post.deleted = false
            """)
    long countByPostAndIsFavoriteTrue(@Param("post") Post post);

    @Query("""
                SELECT pf.post.id, COUNT(pf)
                FROM PostFavorite pf
                WHERE pf.post IN :posts AND pf.isFavorite = true
                GROUP BY pf.post.id
            """)
    List<Object[]> countFavoritesByPosts(@Param("posts") List<Post> posts);

    @Query("""
                select pf.post.id
                from PostFavorite pf
                where pf.user = :user
                  and pf.post.id in :postIds
                  and pf.isFavorite = true
                  and pf.post.deleted = false
            """)
    List<Long> findFavoritePostIdsByUserAndPostIds(@Param("user") User user, @Param("postIds") List<Long> postIds);

    @Query("""
                SELECT pf
                FROM PostFavorite pf
                JOIN FETCH pf.post p
                WHERE pf.user = :user
                  AND pf.isFavorite = true
                  AND p.deleted = false
                ORDER BY pf.id DESC
            """)
    Slice<PostFavorite> findByUserAndIsFavoriteTrueOrderByIdDesc(@Param("user") User user, Pageable pageable);

    @Query("""
                SELECT pf
                FROM PostFavorite pf
                JOIN FETCH pf.post p
                WHERE pf.user = :user
                  AND pf.isFavorite = true
                  AND p.deleted = false
                  AND pf.id < :cursor
                ORDER BY pf.id DESC
            """)
    Slice<PostFavorite> findByUserAndIsFavoriteTrueAndIdLessThanOrderByIdDesc(
            @Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);

    // 활동 내역용 - 날짜/키워드 필터 포함
    @Query("""
                SELECT pf FROM PostFavorite pf JOIN FETCH pf.post p
                WHERE pf.user = :user AND pf.isFavorite = true AND p.deleted = false
                  AND (:startDate IS NULL OR pf.createdAt >= :startDate)
                  AND (:endDate IS NULL OR pf.createdAt < :endDate)
                  AND (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%'))
                  AND (:cursor IS NULL OR pf.createdAt < :cursor)
                ORDER BY pf.createdAt DESC
            """)
    Slice<PostFavorite> findUserActivityFavorites(
            @Param("user") User user,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("keyword") String keyword,
            @Param("cursor") java.time.LocalDateTime cursor,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostFavorite pf where pf.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);

    @Query("""
                SELECT pf
                FROM PostFavorite pf
                JOIN FETCH pf.post p
                WHERE pf.user = :user
                  AND pf.isFavorite = true
                  AND p.deleted = false
                ORDER BY pf.createdAt DESC
            """)
    Slice<PostFavorite> findByUserAndIsFavoriteTrueOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    @Query("""
                SELECT pf
                FROM PostFavorite pf
                JOIN FETCH pf.post p
                WHERE pf.user = :user
                  AND pf.isFavorite = true
                  AND p.deleted = false
                  AND pf.createdAt < :cursor
                ORDER BY pf.createdAt DESC
            """)
    Slice<PostFavorite> findByUserAndIsFavoriteTrueAndCreatedAtBeforeOrderByCreatedAtDesc(
            @Param("user") User user, @Param("cursor") java.time.LocalDateTime cursor, Pageable pageable);

    @Modifying
    @Query("""
                delete from PostFavorite pf
                where pf.post.id in :postIds
            """)
    void deleteAllByPostIds(@Param("postIds") List<Long> postIds);
}
