package com.fmi.domain.noticelike.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.noticelike.data.NoticeLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoticeLikeRepository extends JpaRepository<NoticeLike, Long> {

    Optional<NoticeLike> findByUserAndNotice(User user, Notice notice);

    void deleteByNoticeNoticeId(Long noticeId);
}
