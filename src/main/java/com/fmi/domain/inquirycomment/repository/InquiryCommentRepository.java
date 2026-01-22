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
    @Query("SELECT c FROM InquiryComment c WHERE c.inquiry.id = :inquiryId AND c.parent IS NULL AND c.id < :cursor ORDER BY c.id DESC")
    Slice<InquiryComment> findByInquiryIdAndIdLessThanOrderByIdDesc(@Param("inquiryId") Long inquiryId, @Param("cursor") Long cursor, Pageable pageable);

    /**
     * 특정 댓글의 대댓글 목록 조회
     */
    @Query("SELECT c FROM InquiryComment c WHERE c.parent.id = :parentId ORDER BY c.id ASC")
    java.util.List<InquiryComment> findByParentIdOrderByIdAsc(@Param("parentId") Long parentId);
}
