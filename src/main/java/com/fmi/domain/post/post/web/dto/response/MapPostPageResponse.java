package com.fmi.domain.post.post.web.dto.response;

import java.util.List;

public record MapPostPageResponse(List<MapPostResponse> posts, boolean hasNext, Double nextDistance, Long nextPostId) {}
