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

import java.util.List;

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

    // 사용자별 커서 기반 조회 - 전체
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user = :user
              AND (:cursor IS NULL OR i.id < :cursor)
            ORDER BY i.id DESC
            """)
    List<Inquiry> findByUserCursor(@Param("user") User user,
                                    @Param("cursor") Long cursor,
                                    Pageable pageable);

    // 사용자별 커서 기반 조회 - 상태 필터
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user = :user AND i.answerStatus = :status
              AND (:cursor IS NULL OR i.id < :cursor)
            ORDER BY i.id DESC
            """)
    List<Inquiry> findByUserAndAnswerStatusCursor(@Param("user") User user,
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
    List<Inquiry> findByUserAndAnswerStatusNotCursor(@Param("user") User user,
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
            """,
            nativeQuery = true)
    List<Inquiry> findByUserAndKeywordCursor(@Param("user") User user,
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
            """,
            nativeQuery = true)
    List<Inquiry> findByUserAndAnswerStatusAndKeywordCursor(@Param("user") User user,
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
            """,
            nativeQuery = true)
    List<Inquiry> findByUserAndAnswerStatusNotAndKeywordCursor(@Param("user") User user,
                                                                @Param("status") InquiryStatus status,
                                                                @Param("keyword") String keyword,
                                                                @Param("cursor") Long cursor,
                                                                Pageable pageable);

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

    // 관리자 회원 문의 전용 - keyword 없을 때
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user IS NOT NULL
              AND (:type IS NULL OR i.inquiryType = :type)
              AND (:status IS NULL OR i.answerStatus = :status)
            """)
    Page<Inquiry> findMemberInquiriesForAdmin(@Param("type") InquiryType type,
                                              @Param("status") InquiryStatus status,
                                              Pageable pageable);

    // 관리자 회원 문의 전용 - keyword 있을 때
    @Query(value = """
            SELECT * FROM customer_inquiry i
            WHERE i.user_id IS NOT NULL
              AND (:type IS NULL OR i.inquiry_type = :type)
              AND (:status IS NULL OR i.answer_status = :status)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY i.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM customer_inquiry i
            WHERE i.user_id IS NOT NULL
              AND (:type IS NULL OR i.inquiry_type = :type)
              AND (:status IS NULL OR i.answer_status = :status)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            """,
            nativeQuery = true)
    Page<Inquiry> findMemberInquiriesForAdminWithKeyword(@Param("type") String type,
                                                         @Param("status") String status,
                                                         @Param("keyword") String keyword,
                                                         Pageable pageable);

    // 관리자 비회원 문의 전용 - keyword 없을 때
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user IS NULL
              AND (:status IS NULL OR i.answerStatus = :status)
            """)
    Page<Inquiry> findGuestInquiriesForAdmin(@Param("status") InquiryStatus status,
                                             Pageable pageable);

    // 관리자 비회원 문의 전용 - keyword 있을 때
    @Query(value = """
            SELECT * FROM customer_inquiry i
            WHERE i.user_id IS NULL
              AND (:status IS NULL OR i.answer_status = :status)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY i.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM customer_inquiry i
            WHERE i.user_id IS NULL
              AND (:status IS NULL OR i.answer_status = :status)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            """,
            nativeQuery = true)
    Page<Inquiry> findGuestInquiriesForAdminWithKeyword(@Param("status") String status,
                                                        @Param("keyword") String keyword,
                                                        Pageable pageable);

    // 통합 목록용 커서 기반 - 회원 문의만
    @Query("SELECT i FROM Inquiry i WHERE i.user IS NOT NULL ORDER BY i.createdAt DESC")
    Slice<Inquiry> findMemberInquiriesOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT i FROM Inquiry i WHERE i.user IS NOT NULL AND i.createdAt < :cursor ORDER BY i.createdAt DESC")
    Slice<Inquiry> findMemberInquiriesBeforeCursorOrderByCreatedAtDesc(@Param("cursor") java.time.LocalDateTime cursor, Pageable pageable);

    // 관리자 비회원 문의 커서 기반 (id 기반, 무한스크롤) - keyword 없을 때
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user IS NULL
              AND (:status IS NULL OR i.answerStatus = :status)
            ORDER BY i.id DESC
            """)
    Slice<Inquiry> findGuestInquiriesOrderByIdDesc(@Param("status") InquiryStatus status, Pageable pageable);

    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user IS NULL
              AND (:status IS NULL OR i.answerStatus = :status)
              AND i.id < :cursor
            ORDER BY i.id DESC
            """)
    Slice<Inquiry> findGuestInquiriesBeforeCursorOrderByIdDesc(@Param("status") InquiryStatus status,
                                                               @Param("cursor") Long cursor,
                                                               Pageable pageable);

    // 관리자 비회원 문의 - answered=false (미답변)
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user IS NULL
              AND i.answerStatus <> com.fmi.domain.inquiry.data.enums.InquiryStatus.ANSWERED
            ORDER BY i.id DESC
            """)
    Slice<Inquiry> findGuestInquiriesNotAnsweredOrderByIdDesc(Pageable pageable);

    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user IS NULL
              AND i.answerStatus <> com.fmi.domain.inquiry.data.enums.InquiryStatus.ANSWERED
              AND i.id < :cursor
            ORDER BY i.id DESC
            """)
    Slice<Inquiry> findGuestInquiriesNotAnsweredBeforeCursorOrderByIdDesc(@Param("cursor") Long cursor,
                                                                          Pageable pageable);

    // 관리자 비회원 문의 - answered=false + keyword (FULLTEXT, Slice)
    @Query(value = """
            SELECT * FROM customer_inquiry i
            WHERE i.user_id IS NULL
              AND i.answer_status <> 'ANSWERED'
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
              AND (:cursor IS NULL OR i.id < :cursor)
            ORDER BY i.id DESC
            """,
            nativeQuery = true)
    Slice<Inquiry> findGuestInquiriesNotAnsweredWithKeywordSlice(@Param("keyword") String keyword,
                                                                 @Param("cursor") Long cursor,
                                                                 Pageable pageable);

    // 관리자 비회원 문의 커서 기반 (id 기반) - keyword 있을 때 (FULLTEXT, Slice)
    @Query(value = """
            SELECT * FROM customer_inquiry i
            WHERE i.user_id IS NULL
              AND (:status IS NULL OR i.answer_status = :status)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
              AND (:cursor IS NULL OR i.id < :cursor)
            ORDER BY i.id DESC
            """,
            nativeQuery = true)
    Slice<Inquiry> findGuestInquiriesForAdminWithKeywordSlice(@Param("status") String status,
                                                              @Param("keyword") String keyword,
                                                              @Param("cursor") Long cursor,
                                                              Pageable pageable);

    // 관리자 커서 기반 조회 - keyword 없을 때 (JPQL, 회원 문의만)
    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.user IS NOT NULL
              AND (:type IS NULL OR i.inquiryType = :type)
              AND (:status IS NULL OR i.answerStatus = :status)
              AND (:answered IS NULL OR i.answered = :answered)
              AND (:cursor IS NULL OR i.id < :cursor)
            ORDER BY i.id DESC
            """)
    List<Inquiry> findAllForAdminCursor(@Param("type") InquiryType type,
                                        @Param("status") InquiryStatus status,
                                        @Param("answered") Boolean answered,
                                        @Param("cursor") Long cursor,
                                        Pageable pageable);

    // 관리자 커서 기반 조회 - keyword 있을 때 (FULLTEXT + ngram, 회원 문의만)
    @Query(value = """
            SELECT * FROM customer_inquiry i
            WHERE i.user_id IS NOT NULL
              AND (:type IS NULL OR i.inquiry_type = :type)
              AND (:status IS NULL OR i.answer_status = :status)
              AND (:answered IS NULL OR i.answered = :answered)
              AND (:cursor IS NULL OR i.id < :cursor)
              AND MATCH(i.title, i.content) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY i.id DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<Inquiry> findAllForAdminWithKeywordCursor(@Param("type") String type,
                                                    @Param("status") String status,
                                                    @Param("answered") Boolean answered,
                                                    @Param("keyword") String keyword,
                                                    @Param("cursor") Long cursor,
                                                    @Param("limit") int limit);
}

