package com.fmi.domain.noticecommentlike.repository;

import com.fmi.domain.noticecomment.data.NoticeComment;
import com.fmi.domain.noticecommentlike.data.NoticeCommentLike;
import com.fmi.domain.user.data.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeCommentLikeRepository extends JpaRepository<NoticeCommentLike, Long> {

    Optional<NoticeCommentLike> findByUserAndComment(User user, NoticeComment comment);

    @Query(
            "SELECT ncl.comment.id FROM NoticeCommentLike ncl WHERE ncl.user.id = :userId AND ncl.comment.id IN :commentIds")
    Set<Long> findByUserIdAndCommentIdIn(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NoticeCommentLike ncl WHERE ncl.comment.id IN "
            + "(SELECT c.id FROM NoticeComment c WHERE c.notice.noticeId = :noticeId)")
    void deleteAllByNoticeId(@Param("noticeId") Long noticeId);
}
