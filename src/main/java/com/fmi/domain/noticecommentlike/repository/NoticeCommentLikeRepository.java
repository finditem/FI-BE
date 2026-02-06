package com.fmi.domain.noticecommentlike.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.noticecomment.data.NoticeComment;
import com.fmi.domain.noticecommentlike.data.NoticeCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoticeCommentLikeRepository extends JpaRepository<NoticeCommentLike, Long> {

    Optional<NoticeCommentLike> findByUserAndComment(User user, NoticeComment comment);
}
