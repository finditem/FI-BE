package com.fmi.domain.review.repository;

import com.fmi.domain.review.data.ReviewHelpType;
import com.fmi.domain.review.data.ReviewSummary;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ReviewRepositoryCustom {

    List<ReviewSummary> findReceivedReviews(Long recipientId, boolean excludeHidden, Long cursor, int limit);

    long countReceivedReviews(Long recipientId, boolean excludeHidden);

    List<ReviewSummary> findSentReviews(Long authorId, Long cursor, int limit);

    long countSentReviews(Long authorId);

    Map<Long, Set<ReviewHelpType>> findHelpTypesByReviewIds(List<Long> reviewIds);
}
