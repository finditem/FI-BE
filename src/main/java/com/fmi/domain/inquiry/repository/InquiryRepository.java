package com.fmi.domain.inquiry.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 관리자 전용 조회: 조건이 없으면 전체
    @Query("""
            SELECT i FROM Inquiry i
            WHERE (:type IS NULL OR i.inquiryType = :type)
              AND (:status IS NULL OR i.answerStatus = :status)
              AND (:category IS NULL OR i.category = :category)
            """)
    Page<Inquiry> findAllForAdmin(@Param("type") InquiryType type,
                                  @Param("status") InquiryStatus status,
                                  @Param("category") InquiryCategory category,
                                  Pageable pageable);
}

