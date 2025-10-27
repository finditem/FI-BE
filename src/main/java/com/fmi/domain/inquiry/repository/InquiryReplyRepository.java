package com.fmi.domain.inquiry.repository;

import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.data.InquiryReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InquiryReplyRepository extends JpaRepository<InquiryReply, Long> {
    
    // 문의에 대한 답변 조회
    Optional<InquiryReply> findByInquiry(Inquiry inquiry);
    
    // 답변 존재 여부
    boolean existsByInquiry(Inquiry inquiry);
}

