package com.fmi.domain.post.web.dto.response;

public record PostShareResponse(
        String title,
        String summary,
        String thumbnailUrl
) {
}
