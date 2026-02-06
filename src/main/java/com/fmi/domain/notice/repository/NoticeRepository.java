package com.fmi.domain.notice.repository;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.enums.NoticeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 카테고리별 조회
    Page<Notice> findByCategory(NoticeCategory category, Pageable pageable);

    // 상단 고정 공지사항 조회
    Page<Notice> findByPinnedTrue(Pageable pageable);

    // 키워드 검색 (제목, 내용)
    @Query("""
            SELECT n FROM Notice n
            WHERE n.draft = false
              AND (:category IS NULL OR n.category = :category)
              AND (:keyword IS NULL OR n.title LIKE CONCAT('%', :keyword, '%')
                   OR n.content LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Notice> searchNotices(@Param("category") NoticeCategory category,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    // draft=false인 공지만 조회
    Page<Notice> findByDraftFalse(Pageable pageable);

    // draft=false + 카테고리 필터
    Page<Notice> findByDraftFalseAndCategory(NoticeCategory category, Pageable pageable);

    // 임시저장 목록 (관리자용)
    Page<Notice> findByDraftTrue(Pageable pageable);
}

