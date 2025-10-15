package com.fmi.domain.post.repository;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.User;
import com.fmi.domain.post.data.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post,Long> {


    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.images WHERE p.user = :user AND p.temporarySave = true")
    Optional<Post> findByUserAndTemporarySaveTrue(@Param("user") User user);

//    Page<Post> findByTemporarySaveFalse(Pageable pageable);

    Page<Post> findByTemporarySaveFalseAndPostType(Type postType, Pageable pageable);

    Optional<Post> deleteByUserAndTemporarySaveTrue(User user);

}
