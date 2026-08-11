package com.fmi.domain.noticecomment.repository;

import com.fmi.domain.noticecomment.data.NoticeComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeCommentRepository extends JpaRepository<NoticeComment, Long> {

    long countByParentId(Long parentId);

    @Query("SELECT c.parent.id, COUNT(c) FROM NoticeComment c WHERE c.parent.id IN :parentIds GROUP BY c.parent.id")
    List<Object[]> countByParentIds(@Param("parentIds") List<Long> parentIds);

    @Query("select c from NoticeComment c join fetch c.user left join fetch c.parent " +
            "where c.notice.noticeId = :noticeId and c.parent is null order by c.id desc")
    Slice<NoticeComment> findTopByNoticeIdOrderByIdDesc(@Param("noticeId") Long noticeId, Pageable pageable);

    @Query("select c from NoticeComment c join fetch c.user left join fetch c.parent " +
            "where c.notice.noticeId = :noticeId and c.parent is null and c.id < :cursor order by c.id desc")
    Slice<NoticeComment> findByNoticeIdAndIdLessThanOrderByIdDesc(@Param("noticeId") Long noticeId,
                                                                  @Param("cursor") Long cursor,
                                                                  Pageable pageable);

    @Query(value = "select c from NoticeComment c join fetch c.user " +
            "where c.parent.id = :parentId order by c.id asc",
           countQuery = "select count(c) from NoticeComment c where c.parent.id = :parentId")
    Page<NoticeComment> findRepliesByParentId(@Param("parentId") Long parentId, Pageable pageable);

    long countByNoticeNoticeId(Long noticeId);

    @Query("SELECT c.notice.noticeId, COUNT(c) FROM NoticeComment c WHERE c.notice.noticeId IN :noticeIds GROUP BY c.notice.noticeId")
    List<Object[]> countByNoticeIds(@Param("noticeIds") List<Long> noticeIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NoticeComment c WHERE c.notice.noticeId = :noticeId")
    void deleteByNoticeNoticeId(@Param("noticeId") Long noticeId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE NoticeComment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :commentId")
    void incrementLikeCount(@Param("commentId") Long commentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE NoticeComment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :commentId AND c.likeCount > 0")
    void decrementLikeCount(@Param("commentId") Long commentId);
}
