package com.fmi.domain.map.web.dto.response;

import java.util.List;

public record LocationMapPostPageResponse(
        List<MapPostResponse> posts, boolean hasNext, Double nextDistance, Long nextPostId) {}
