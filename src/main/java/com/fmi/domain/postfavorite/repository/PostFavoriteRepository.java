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

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite,Long> {
    Optional<PostFavorite> findByUserAndPost(User user, Post post);
    List<PostFavorite> findByUserAndIsFavoriteTrue(User user);

    @Query("SELECT pf.user FROM PostFavorite pf WHERE pf.post = :post AND pf.isFavorite = true")
    List<User> findUsersByPost(@Param("post") Post post);
}
