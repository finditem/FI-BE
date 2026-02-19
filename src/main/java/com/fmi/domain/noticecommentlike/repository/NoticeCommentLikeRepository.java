package com.fmi.domain.noticecommentlike.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.noticecomment.data.NoticeComment;
import com.fmi.domain.noticecommentlike.data.NoticeCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoticeCommentLikeRepository extends JpaRepository<NoticeCommentLike, Long> {

    Optional<NoticeCommentLike> findByUserAndComment(User user, NoticeComment comment);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NoticeCommentLike ncl WHERE ncl.comment.id IN " +
            "(SELECT c.id FROM NoticeComment c WHERE c.notice.noticeId = :noticeId)")
    void deleteAllByNoticeId(@Param("noticeId") Long noticeId);
}
