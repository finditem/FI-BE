package com.fmi.domain.post.web.dto.response;

import java.util.List;

public record PostPageResponse(List<PostBriefResponse> postList, Long postCount, Long nextCursor, boolean hasNext) {}
