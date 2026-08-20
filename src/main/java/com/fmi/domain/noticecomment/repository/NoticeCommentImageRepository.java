package com.fmi.domain.noticecomment.repository;

import com.fmi.domain.noticecomment.data.NoticeCommentImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeCommentImageRepository extends JpaRepository<NoticeCommentImage, Long> {

    List<NoticeCommentImage> findByComment_IdIn(List<Long> commentIds);

    List<NoticeCommentImage> findByComment_Id(Long commentId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NoticeCommentImage i WHERE i.comment.id = :commentId")
    void deleteAllByCommentId(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true)
    @Query(
            "DELETE FROM NoticeCommentImage i WHERE i.comment.id IN (SELECT c.id FROM NoticeComment c WHERE c.notice.noticeId = :noticeId)")
    void deleteAllByNoticeId(@Param("noticeId") Long noticeId);
}
