package com.fmi.domain.commentlike.repository;

import com.fmi.domain.commentlike.data.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<CommentLike,Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
