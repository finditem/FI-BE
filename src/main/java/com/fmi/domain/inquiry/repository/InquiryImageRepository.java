package com.fmi.domain.inquiry.repository;

import com.fmi.domain.inquiry.data.InquiryImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryImageRepository extends JpaRepository<InquiryImage, Long> {

    List<InquiryImage> findByInquiryId(Long inquiryId);

    @Query("SELECT i FROM InquiryImage i WHERE i.inquiry.id IN :inquiryIds")
    List<InquiryImage> findByInquiryIdIn(@Param("inquiryIds") List<Long> inquiryIds);

    void deleteAllByInquiryId(Long inquiryId);
}
