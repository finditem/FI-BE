package com.fmi.domain.review.repository;

import com.fmi.domain.review.data.Review;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

    boolean existsByFindCompletionId(Long findCompletionId);

    // 후기 상세에 필요한 사용자, 도움 경험, 찾기 완료와 채팅방을 함께 조회한다.
    @Query("SELECT r FROM Review r "
            + "LEFT JOIN FETCH r.author "
            + "LEFT JOIN FETCH r.recipient "
            + "LEFT JOIN FETCH r.helpTypes "
            + "JOIN FETCH r.findCompletion fc "
            + "LEFT JOIN FETCH fc.chatRoom "
            + "WHERE r.id = :reviewId")
    Optional<Review> findDetailById(@Param("reviewId") Long reviewId);
}
