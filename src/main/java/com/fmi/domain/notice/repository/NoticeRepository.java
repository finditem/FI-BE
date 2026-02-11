package com.fmi.domain.notice.repository;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.enums.NoticeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 상단 고정 공지사항 조회 (발행된 것만)
    Page<Notice> findByDraftFalseAndPinnedTrue(Pageable pageable);

    // 키워드 검색 (제목, 내용) - FULLTEXT INDEX + ngram
    @Query(value = """
            SELECT * FROM notice n
            WHERE n.draft = false
              AND (:category IS NULL OR n.category = :category)
              AND MATCH(n.title, n.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY n.pinned DESC, n.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM notice n
            WHERE n.draft = false
              AND (:category IS NULL OR n.category = :category)
              AND MATCH(n.title, n.content) AGAINST(:keyword IN BOOLEAN MODE)
            """,
            nativeQuery = true)
    Page<Notice> searchNotices(@Param("category") String category,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    // draft=false인 공지만 조회
    Page<Notice> findByDraftFalse(Pageable pageable);

    // draft=false + 카테고리 필터
    Page<Notice> findByDraftFalseAndCategory(NoticeCategory category, Pageable pageable);

    // 임시저장 목록 (관리자용)
    Page<Notice> findByDraftTrue(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notice n WHERE n.noticeId = :noticeId")
    Optional<Notice> findByIdWithLock(@Param("noticeId") Long noticeId);
}

