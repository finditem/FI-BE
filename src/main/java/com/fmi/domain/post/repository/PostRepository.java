package com.fmi.domain.post.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.map.repository.PostMapCustom;
import com.fmi.domain.post.data.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom, PostMapCustom {

    Optional<Post> findByIdAndDeletedFalse(Long id);

    @Modifying(flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId and p.deleted = false")
    void increaseViewCount(@Param("postId") Long postId);

//    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.images WHERE p.user = :user AND p.temporarySave = true")
//    Optional<Post> findByUserAndTemporarySaveTrue(@Param("user") User user);

//    Page<Post> findByTemporarySaveFalse(Pageable pageable);

//    Slice<Post> findByTemporarySaveFalseAndPostType(Type postType, Pageable pageable);

    // 2. 두 번째 페이지부터용 (마지막으로 본 ID보다 작은 데이터들 조회)
//    Slice<Post> findByTemporarySaveFalseAndPostTypeAndIdLessThan(Type postType, Long id, Pageable pageable);

//    Optional<Post> deleteByUserAndTemporarySaveTrue(User user);

//    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.images WHERE p.user.email = :email AND p.temporarySave = true")
//    Optional<Post> findByUserEmailAndTemporarySaveTrue(@Param("email") String email);

    // 특정 사용자의 게시글 조회 (익명화 처리용)
    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.deleted = false")
    List<Post> findByUser(@Param("user") User user);

    @Query("SELECT DISTINCT p FROM Post p WHERE p.user = :user AND p.temporarySave = false AND p.deleted = false")
    List<Post> findAllPublishedWithImagesByUser(@Param("user") User user);

    Slice<Post> findByUserAndTemporarySaveFalseAndDeletedFalseOrderByIdDesc(User user, Pageable pageable);

    Slice<Post> findByUserAndTemporarySaveFalseAndDeletedFalseAndIdLessThanOrderByIdDesc(User user, Long cursor, Pageable pageable);

    long countByUserAndDeletedFalse(User user);


//    @Modifying
//    @Query(value = """
//                UPDATE Post p SET p.viewCnt = p.viewCnt + :#{#counts[p.id]}
//                WHERE p.id IN :#{#counts.keySet()}
//            """)
//    void batchIncrementViewCounts(@Param("counts") Map<Long, Long> counts);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.temporarySave = false AND p.deleted = false AND p.createdAt < :cursor ORDER BY p.createdAt DESC")
    Slice<Post> findByUserAndTemporarySaveFalseAndDeletedFalseAndCreatedAtBeforeOrderByCreatedAtDesc(@Param("user") User user, @Param("cursor") LocalDateTime cursor, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.temporarySave = false AND p.deleted = false ORDER BY p.createdAt DESC")
    Slice<Post> findByUserAndTemporarySaveFalseAndDeletedFalseOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    Long countByUserAndTemporarySaveFalseAndDeletedFalse(User user);

    Optional<Post> findByUserAndTemporarySaveTrueAndDeletedFalse(User user);

    // 마케팅 동의 유저 게시글 - 최신순
    @Query(value = """
            SELECT p.* FROM post p
            INNER JOIN users u ON p.user_id = u.id
            WHERE u.marketing_consent = true AND u.deleted = false
              AND p.temporary_save = false AND p.deleted = false
              AND (:category IS NULL OR p.category = :category)
              AND (:postStatus IS NULL OR p.item_status = :postStatus)
              AND (:cursor IS NULL OR p.id < :cursor)
            ORDER BY p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Post> findMarketingConsentPostsByLatest(@Param("category") String category,
                                                  @Param("postStatus") String postStatus,
                                                  @Param("cursor") Long cursor,
                                                  @Param("limit") int limit);

    // 마케팅 동의 유저 게시글 - 인기순 (view_count + id 복합 커서)
    @Query(value = """
            SELECT p.* FROM post p
            INNER JOIN users u ON p.user_id = u.id
            WHERE u.marketing_consent = true AND u.deleted = false
              AND p.temporary_save = false AND p.deleted = false
              AND (:category IS NULL OR p.category = :category)
              AND (:postStatus IS NULL OR p.item_status = :postStatus)
              AND (:cursorViewCount IS NULL OR (p.view_count < :cursorViewCount OR (p.view_count = :cursorViewCount AND p.id < :cursorId)))
            ORDER BY p.view_count DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Post> findMarketingConsentPostsByPopular(@Param("category") String category,
                                                   @Param("postStatus") String postStatus,
                                                   @Param("cursorViewCount") Long cursorViewCount,
                                                   @Param("cursorId") Long cursorId,
                                                   @Param("limit") int limit);

}
