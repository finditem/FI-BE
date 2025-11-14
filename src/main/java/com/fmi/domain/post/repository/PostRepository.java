package com.fmi.domain.post.repository;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post,Long> {


    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.images WHERE p.user = :user AND p.temporarySave = true")
    Optional<Post> findByUserAndTemporarySaveTrue(@Param("user") User user);

//    Page<Post> findByTemporarySaveFalse(Pageable pageable);

    Page<Post> findByTemporarySaveFalseAndPostType(Type postType, Pageable pageable);

    Optional<Post> deleteByUserAndTemporarySaveTrue(User user);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.images WHERE p.user.email = :email AND p.temporarySave = true")
    Optional<Post> findByUserEmailAndTemporarySaveTrue(@Param("email") String email);
    
    // 특정 사용자의 게시글 조회 (익명화 처리용)
    @Query("SELECT p FROM Post p WHERE p.user = :user")
    List<Post> findByUser(@Param("user") User user);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.images WHERE p.user = :user AND p.temporarySave = false")
    List<Post> findAllPublishedWithImagesByUser(@Param("user") User user);

    long countByUser(User user);

    @Query("update Post p set p.viewCnt = p.viewCnt + :count where p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId, @Param("count") Long count);
}
