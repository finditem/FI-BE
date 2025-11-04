package com.fmi.domain.notice.repository;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.enums.NoticeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    
    // 카테고리별 조회
    Page<Notice> findByCategory(NoticeCategory category, Pageable pageable);
    
    // 상단 고정 공지사항 조회
    Page<Notice> findByPinnedTrue(Pageable pageable);
}

