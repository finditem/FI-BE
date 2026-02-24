package com.fmi.domain.notice.repository;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.enums.NoticeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 키워드 검색 - 최신순 (기본)
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

    // 키워드 검색 - 오래된순
    @Query(value = """
            SELECT * FROM notice n
            WHERE n.draft = false
              AND (:category IS NULL OR n.category = :category)
              AND MATCH(n.title, n.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY n.pinned DESC, n.created_at ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM notice n
            WHERE n.draft = false
              AND (:category IS NULL OR n.category = :category)
              AND MATCH(n.title, n.content) AGAINST(:keyword IN BOOLEAN MODE)
            """,
            nativeQuery = true)
    Page<Notice> searchNoticesOldest(@Param("category") String category,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);

    // 키워드 검색 - 조회많은순
    @Query(value = """
            SELECT * FROM notice n
            WHERE n.draft = false
              AND (:category IS NULL OR n.category = :category)
              AND MATCH(n.title, n.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY n.pinned DESC, n.view_cnt DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM notice n
            WHERE n.draft = false
              AND (:category IS NULL OR n.category = :category)
              AND MATCH(n.title, n.content) AGAINST(:keyword IN BOOLEAN MODE)
            """,
            nativeQuery = true)
    Page<Notice> searchNoticesMostViewed(@Param("category") String category,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    // draft=false인 공지만 조회
    Page<Notice> findByDraftFalse(Pageable pageable);

    // draft=false + 카테고리 필터
    Page<Notice> findByDraftFalseAndCategory(NoticeCategory category, Pageable pageable);

    // 임시저장 단건 조회 (관리자용 - 유저당 1건)
    Optional<Notice> findFirstByDraftTrueAndAuthorEmailOrderByUpdatedAtDesc(String email);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notice n SET n.likeCount = n.likeCount + 1 WHERE n.noticeId = :noticeId")
    void incrementLikeCount(@Param("noticeId") Long noticeId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notice n SET n.likeCount = n.likeCount - 1 WHERE n.noticeId = :noticeId AND n.likeCount > 0")
    void decrementLikeCount(@Param("noticeId") Long noticeId);
}

