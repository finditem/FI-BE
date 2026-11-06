package com.fmi.domain.inquiry.inquiry.repository;

import com.fmi.domain.inquiry.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryType;
import com.fmi.domain.user.user.data.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 공개 문의 목록 조회
    Page<Inquiry> findByInquiryType(InquiryType inquiryType, Pageable pageable);

    // 공개 문의 + 상태별 조회
    Page<Inquiry> findByInquiryTypeAndAnswerStatus(InquiryType inquiryType, InquiryStatus status, Pageable pageable);

    // 사용자별 문의 목록
    Page<Inquiry> findByUser(User user, Pageable pageable);

    // 사용자별 + 상태별 문의 목록
    Page<Inquiry> findByUserAndAnswerStatus(User user, InquiryStatus status, Pageable pageable);

    // 사용자별 + 특정 상태 제외 문의 목록
    Page<Inquiry> findByUserAndAnswerStatusNot(User user, InquiryStatus status, Pageable pageable);

    // 사용자별 문의 목록 (createdAt 커서 기반)
    @Query("SELECT i FROM Inquiry i WHERE i.user = :user ORDER BY i.createdAt DESC")
    Slice<Inquiry> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT i FROM Inquiry i WHERE i.user = :user AND i.createdAt < :cursor ORDER BY i.createdAt DESC")
    Slice<Inquiry> findByUserAndCreatedAtBeforeOrderByCreatedAtDesc(
            @Param("user") User user, @Param("cursor") java.time.LocalDateTime cursor, Pageable pageable);

    // 활동 내역용 - 날짜/키워드 필터 포함
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user = :user
              AND (:startDate IS NULL OR i.createdAt >= :startDate)
              AND (:endDate IS NULL OR i.createdAt < :endDate)
              AND (:keyword IS NULL OR i.title LIKE CONCAT('%', :keyword, '%')
                   OR i.content LIKE CONCAT('%', :keyword, '%'))
              AND (:cursor IS NULL OR i.createdAt < :cursor)
            ORDER BY i.createdAt DESC
            """)
    Slice<Inquiry> findUserActivityInquiries(
            @Param("user") User user,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("keyword") String keyword,
            @Param("cursor") java.time.LocalDateTime cursor,
            Pageable pageable);

    // 사용자별 커서 기반 조회 - 전체
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user = :user
              AND (:cursor IS NULL OR i.id < :cursor)
            ORDER BY i.id DESC
            """)
    List<Inquiry> findByUserCursor(@Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);

    // 사용자별 커서 기반 조회 - 상태 필터
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user = :user AND i.answerStatus = :status
              AND (:cursor IS NULL OR i.id < :cursor)
            ORDER BY i.id DESC
            """)
    List<Inquiry> findByUserAndAnswerStatusCursor(
            @Param("user") User user,
            @Param("status") InquiryStatus status,
            @Param("cursor") Long cursor,
            Pageable pageable);

    // 사용자별 커서 기반 조회 - 상태 제외 필터
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user = :user AND i.answerStatus <> :status
              AND (:cursor IS NULL OR i.id < :cursor)
            ORDER BY i.id DESC
            """)
    List<Inquiry> findByUserAndAnswerStatusNotCursor(
            @Param("user") User user,
            @Param("status") InquiryStatus status,
            @Param("cursor") Long cursor,
            Pageable pageable);

    // 사용자별 커서 기반 조회 - keyword 검색
    @Query(value = """
            SELECT * FROM customer_inquiry i
            WHERE i.user_id = :#{#user.id}
              AND (:cursor IS NULL OR i.id < :cursor)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY i.id DESC
            """, nativeQuery = true)
    List<Inquiry> findByUserAndKeywordCursor(
            @Param("user") User user,
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            Pageable pageable);

    // 사용자별 커서 기반 조회 - 상태 필터 + keyword 검색
    @Query(value = """
            SELECT * FROM customer_inquiry i
            WHERE i.user_id = :#{#user.id} AND i.answer_status = :#{#status.name()}
              AND (:cursor IS NULL OR i.id < :cursor)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY i.id DESC
            """, nativeQuery = true)
    List<Inquiry> findByUserAndAnswerStatusAndKeywordCursor(
            @Param("user") User user,
            @Param("status") InquiryStatus status,
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            Pageable pageable);

    // 사용자별 커서 기반 조회 - 상태 제외 필터 + keyword 검색
    @Query(value = """
            SELECT * FROM customer_inquiry i
            WHERE i.user_id = :#{#user.id} AND i.answer_status <> :#{#status.name()}
              AND (:cursor IS NULL OR i.id < :cursor)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY i.id DESC
            """, nativeQuery = true)
    List<Inquiry> findByUserAndAnswerStatusNotAndKeywordCursor(
            @Param("user") User user,
            @Param("status") InquiryStatus status,
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            Pageable pageable);
}
