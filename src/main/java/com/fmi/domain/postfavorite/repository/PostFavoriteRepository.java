package com.fmi.domain.postfavorite.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.post.data.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite,Long> {
    Optional<PostFavorite> findByUserAndPost(User user, Post post);

    List<PostFavorite> findByUserAndIsFavoriteTrue(User user);

}
