package com.fmi.domain.noticecomment.repository;

import com.fmi.domain.noticecomment.data.NoticeComment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoticeCommentRepository extends JpaRepository<NoticeComment, Long> {

    @Query("select c from NoticeComment c join fetch c.user left join fetch c.parent " +
            "where c.notice.noticeId = :noticeId order by c.id desc")
    Slice<NoticeComment> findTopByNoticeIdOrderByIdDesc(@Param("noticeId") Long noticeId, Pageable pageable);

    @Query("select c from NoticeComment c join fetch c.user left join fetch c.parent " +
            "where c.notice.noticeId = :noticeId and c.id < :cursor order by c.id desc")
    Slice<NoticeComment> findByNoticeIdAndIdLessThanOrderByIdDesc(@Param("noticeId") Long noticeId,
                                                                  @Param("cursor") Long cursor,
                                                                  Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NoticeComment c WHERE c.notice.noticeId = :noticeId")
    void deleteByNoticeNoticeId(@Param("noticeId") Long noticeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM NoticeComment c WHERE c.id = :commentId")
    Optional<NoticeComment> findByIdWithLock(@Param("commentId") Long commentId);
}
