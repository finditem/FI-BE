package com.fmi.domain.inquirycomment.repository;

import com.fmi.domain.inquirycomment.data.InquiryCommentImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryCommentImageRepository extends JpaRepository<InquiryCommentImage, Long> {

    List<InquiryCommentImage> findByComment_IdIn(List<Long> commentIds);

    List<InquiryCommentImage> findByComment_Id(Long commentId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM InquiryCommentImage i WHERE i.comment.id = :commentId")
    void deleteAllByCommentId(@Param("commentId") Long commentId);
}
