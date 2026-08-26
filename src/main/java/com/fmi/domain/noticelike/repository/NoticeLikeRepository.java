package com.fmi.domain.noticelike.repository;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.noticelike.data.NoticeLike;
import com.fmi.domain.user.data.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeLikeRepository extends JpaRepository<NoticeLike, Long> {

    Optional<NoticeLike> findByUserAndNotice(User user, Notice notice);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NoticeLike nl WHERE nl.notice.noticeId = :noticeId")
    void deleteByNoticeNoticeId(@Param("noticeId") Long noticeId);
}
