package com.fmi.domain.review.repository;

import com.fmi.domain.review.data.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

    boolean existsByFindCompletionId(Long findCompletionId);
}
