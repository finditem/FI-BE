package com.fmi.domain.postfavorite.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.post.data.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite,Long> {
    Optional<PostFavorite> findByUserAndPost(User user, Post post);

    List<PostFavorite> findByUserAndIsFavoriteTrue(User user);

    boolean existsByUserAndPost(User user,Post post);

    @Query("SELECT pf.user FROM PostFavorite pf WHERE pf.post = :post AND pf.isFavorite = true")
    List<User> findUsersByPost(@Param("post") Post post);

    @Query("SELECT pf.post.id FROM PostFavorite pf WHERE pf.user = :user AND pf.post IN :posts AND pf.isFavorite = true")
    Set<Long> findPostIdsByUserAndPostIn(@Param("user") User user, @Param("posts") List<Post> posts);

    @Query("SELECT pf FROM PostFavorite pf JOIN FETCH pf.post p LEFT JOIN FETCH p.images WHERE pf.user = :user AND pf.isFavorite = true ORDER BY pf.id DESC")
    Slice<PostFavorite> findByUserAndIsFavoriteTrueOrderByIdDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT pf FROM PostFavorite pf JOIN FETCH pf.post p LEFT JOIN FETCH p.images WHERE pf.user = :user AND pf.isFavorite = true AND pf.id < :cursor ORDER BY pf.id DESC")
    Slice<PostFavorite> findByUserAndIsFavoriteTrueAndIdLessThanOrderByIdDesc(@Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);
}
