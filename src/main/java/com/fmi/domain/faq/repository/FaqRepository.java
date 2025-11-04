package com.fmi.domain.faq.repository;

import com.fmi.domain.faq.data.Faq;
import com.fmi.domain.faq.data.enums.FaqCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    
    // 카테고리별 조회
    Page<Faq> findByCategory(FaqCategory category, Pageable pageable);
    
    // 카테고리별 전체 조회 (페이징 없음, order_num 순서)
    List<Faq> findByCategoryOrderByOrderNumAsc(FaqCategory category);
    
    // 카테고리별 개수
    long countByCategory(FaqCategory category);
}

