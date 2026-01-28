package com.fmi.domain.postfavorite.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.post.data.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite,Long> {
    Optional<PostFavorite> findByUserAndPost(User user, Post post);

    List<PostFavorite> findByUserAndIsFavoriteTrue(User user);

    @Query("SELECT pf.user FROM PostFavorite pf WHERE pf.post = :post AND pf.isFavorite = true")
    List<User> findUsersByPost(@Param("post") Post post);

    @Query("SELECT pf.post.id FROM PostFavorite pf WHERE pf.user = :user AND pf.post IN :posts AND pf.isFavorite = true")
    Set<Long> findPostIdsByUserAndPostIn(@Param("user") User user, @Param("posts") List<Post> posts);

    long countByPostAndIsFavoriteTrue(Post post);
}
