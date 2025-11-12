package com.fmi.domain.commentlike.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.commentlike.data.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike,Long> {

    Optional<CommentLike> findByUserAndComment(User user , Comment comment);
}
