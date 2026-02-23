package com.fmi.domain.inquiry.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
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
    Slice<Inquiry> findByUserAndCreatedAtBeforeOrderByCreatedAtDesc(@Param("user") User user, @Param("cursor") java.time.LocalDateTime cursor, Pageable pageable);

    // 관리자 전용 조회 - keyword 없을 때 (JPQL)
    @Query("""
            SELECT i FROM Inquiry i
            WHERE (:type IS NULL OR i.inquiryType = :type)
              AND (:status IS NULL OR i.answerStatus = :status)
            """)
    Page<Inquiry> findAllForAdmin(@Param("type") InquiryType type,
                                  @Param("status") InquiryStatus status,
                                  Pageable pageable);

    // 관리자 전용 조회 - keyword 있을 때 (FULLTEXT + ngram)
    @Query(value = """
            SELECT * FROM customer_inquiry i
            WHERE (:type IS NULL OR i.inquiry_type = :type)
              AND (:status IS NULL OR i.answer_status = :status)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY i.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM customer_inquiry i
            WHERE (:type IS NULL OR i.inquiry_type = :type)
              AND (:status IS NULL OR i.answer_status = :status)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            """,
            nativeQuery = true)
    Page<Inquiry> findAllForAdminWithKeyword(@Param("type") String type,
                                             @Param("status") String status,
                                             @Param("keyword") String keyword,
                                             Pageable pageable);
}

