package com.fmi.domain.inquirycomment.repository;

import com.fmi.domain.inquirycomment.data.InquiryComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryCommentRepository extends JpaRepository<InquiryComment, Long> {

    /**
     * 문의의 댓글 목록 조회 (커서 기반 무한스크롤 - 최신순)
     * cursor가 null이면 최신 댓글부터 조회
     */
    @Query("SELECT c FROM InquiryComment c WHERE c.inquiry.id = :inquiryId AND c.parent IS NULL ORDER BY c.id DESC")
    Slice<InquiryComment> findTopByInquiryIdOrderByIdDesc(@Param("inquiryId") Long inquiryId, Pageable pageable);

    /**
     * 문의의 댓글 목록 조회 (커서 기반 무한스크롤 - cursor 이후)
     */
    @Query(
            "SELECT c FROM InquiryComment c WHERE c.inquiry.id = :inquiryId AND c.parent IS NULL AND c.id < :cursor ORDER BY c.id DESC")
    Slice<InquiryComment> findByInquiryIdAndIdLessThanOrderByIdDesc(
            @Param("inquiryId") Long inquiryId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * 특정 댓글의 대댓글 목록 조회
     */
    @Query("SELECT c FROM InquiryComment c WHERE c.parent.id = :parentId ORDER BY c.id ASC")
    java.util.List<InquiryComment> findByParentIdOrderByIdAsc(@Param("parentId") Long parentId);

    /**
     * 문의의 최상위 댓글 전체 조회 (상세 조회용)
     */
    @Query("SELECT c FROM InquiryComment c WHERE c.inquiry.id = :inquiryId AND c.parent IS NULL ORDER BY c.id ASC")
    java.util.List<InquiryComment> findByInquiryIdAndParentIsNullOrderByIdAsc(@Param("inquiryId") Long inquiryId);

    /**
     * 여러 부모 댓글의 대댓글 일괄 조회 (N+1 방지)
     */
    @Query("SELECT c FROM InquiryComment c WHERE c.parent.id IN :parentIds ORDER BY c.id ASC")
    java.util.List<InquiryComment> findByParentIdInOrderByIdAsc(@Param("parentIds") java.util.List<Long> parentIds);

    /**
     * 문의의 모든 댓글 조회 (플랫 리스트)
     */
    @Query("SELECT c FROM InquiryComment c WHERE c.inquiry.id = :inquiryId ORDER BY c.id ASC")
    java.util.List<InquiryComment> findByInquiryIdOrderByIdAsc(@Param("inquiryId") Long inquiryId);

    /**
     * 특정 유저의 문의에 달린 답변 목록 조회 (활동 내역용, createdAt 내림차순)
     */
    @Query("SELECT c FROM InquiryComment c WHERE c.inquiry.user = :user AND c.parent IS NULL ORDER BY c.createdAt DESC")
    Slice<InquiryComment> findByInquiryUserOrderByCreatedAtDesc(
            @Param("user") com.fmi.domain.auth.data.User user, Pageable pageable);

    @Query(
            "SELECT c FROM InquiryComment c WHERE c.inquiry.user = :user AND c.parent IS NULL AND c.createdAt < :cursor ORDER BY c.createdAt DESC")
    Slice<InquiryComment> findByInquiryUserAndCreatedAtBeforeOrderByCreatedAtDesc(
            @Param("user") com.fmi.domain.auth.data.User user,
            @Param("cursor") java.time.LocalDateTime cursor,
            Pageable pageable);

    // 활동 내역용 - 날짜/키워드 필터 포함
    @Query("""
            SELECT c FROM InquiryComment c JOIN FETCH c.inquiry
            WHERE c.inquiry.user = :user AND c.parent IS NULL
              AND (:startDate IS NULL OR c.createdAt >= :startDate)
              AND (:endDate IS NULL OR c.createdAt < :endDate)
              AND (:keyword IS NULL OR c.inquiry.title LIKE CONCAT('%', :keyword, '%')
                   OR c.content LIKE CONCAT('%', :keyword, '%'))
              AND (:cursor IS NULL OR c.createdAt < :cursor)
            ORDER BY c.createdAt DESC
            """)
    Slice<InquiryComment> findUserActivityInquiryAnswers(
            @Param("user") com.fmi.domain.auth.data.User user,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("keyword") String keyword,
            @Param("cursor") java.time.LocalDateTime cursor,
            Pageable pageable);
}
