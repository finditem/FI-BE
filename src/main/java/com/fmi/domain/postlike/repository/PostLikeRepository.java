package com.fmi.domain.postlike.repository;

import com.fmi.domain.postlike.data.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike,Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
