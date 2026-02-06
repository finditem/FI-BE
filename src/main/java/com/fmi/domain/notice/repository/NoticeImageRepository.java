package com.fmi.domain.notice.repository;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.NoticeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long> {

    List<NoticeImage> findByNotice(Notice notice);

    @Query("""
            SELECT ni FROM NoticeImage ni
            WHERE ni.notice.noticeId IN :noticeIds
              AND ni.imageType = com.fmi.domain.post.data.ImageType.THUMBNAIL
            """)
    List<NoticeImage> findThumbnailsByNoticeIds(@Param("noticeIds") List<Long> noticeIds);

    @Modifying
    @Query("DELETE FROM NoticeImage ni WHERE ni.notice = :notice")
    void deleteAllByNotice(@Param("notice") Notice notice);
}
