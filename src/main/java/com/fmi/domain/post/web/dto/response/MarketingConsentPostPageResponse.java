package com.fmi.domain.post.web.dto.response;

import java.util.List;

public record MarketingConsentPostPageResponse(
        List<PostBriefResponse> postList,
        Long nextCursor,
        boolean hasNext
) {
}
