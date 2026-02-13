package com.fmi.domain.post.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
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
    @Query("SELECT p FROM Post p WHERE p.user = :user")
    List<Post> findByUser(@Param("user") User user);

    @Query("SELECT DISTINCT p FROM Post p WHERE p.user = :user AND p.temporarySave = false")
    List<Post> findAllPublishedWithImagesByUser(@Param("user") User user);

    Slice<Post> findByUserAndTemporarySaveFalseOrderByIdDesc(User user, Pageable pageable);

    Slice<Post> findByUserAndTemporarySaveFalseAndIdLessThanOrderByIdDesc(User user, Long cursor, Pageable pageable);

    long countByUser(User user);


//    @Modifying
//    @Query(value = """
//                UPDATE Post p SET p.viewCnt = p.viewCnt + :#{#counts[p.id]}
//                WHERE p.id IN :#{#counts.keySet()}
//            """)
//    void batchIncrementViewCounts(@Param("counts") Map<Long, Long> counts);

//    @Query("""
//                SELECT p FROM Post p
//                WHERE p.temporarySave = false
//                ORDER BY p.commentCount DESC, p.viewCnt DESC, p.favoriteCount DESC
//            """)
//    List<Post> findHotPost(Pageable pageable);

    Long countByUserAndTemporarySaveFalse(User user);

    @Query(value = """
            SELECT *
            FROM post
            WHERE MATCH(title, content)
            AGAINST (:keyword IN BOOLEAN MODE)
              AND id < :cursor
            ORDER BY id DESC
            LIMIT :size
            """, nativeQuery = true)
    List<Post> searchByKeywordWithCursor(@Param("keyword") String keyword,
                                         @Param("cursor") Long cursor,
                                         @Param("size") int size);

    @Query(value = """
            SELECT *
            FROM post
            WHERE MATCH(title, content)
            AGAINST (:keyword IN BOOLEAN MODE)
            ORDER BY id DESC
            LIMIT :size
            """, nativeQuery = true)
    List<Post> searchByKeyword(@Param("keyword") String keyword,
                               @Param("size") int size);

    @Query(value = """
            SELECT COUNT(*)
            FROM post
            WHERE MATCH(title, content)
            AGAINST (:keyword IN BOOLEAN MODE)
            """, nativeQuery = true)
    long countByKeywordFulltext(@Param("keyword") String keyword);

}
