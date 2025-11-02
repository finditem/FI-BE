package com.fmi.domain.comment.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {

    List<Comment> findByPostId(Long postid);
    
    // 특정 사용자의 댓글 조회 (익명화 처리용)
    List<Comment> findByUser(User user);
}
