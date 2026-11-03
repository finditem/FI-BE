package com.fmi.domain.review.data;

import com.fmi.domain.review.data.enums.ReviewHelpType;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ReviewPage(
        List<ReviewSummary> reviews,
        Map<Long, Set<ReviewHelpType>> helpTypesByReviewId,
        Long totalCount,
        Long nextCursor,
        boolean hasNext) {}
