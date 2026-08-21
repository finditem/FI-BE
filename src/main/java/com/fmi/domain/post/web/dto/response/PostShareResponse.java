package com.fmi.domain.post.web.dto.response;

import com.fmi.domain.post.data.PostType;

public record PostShareResponse(
        String title,
        String summary,
        String thumbnailUrl,
        String address,
        Long likeCount,
        Long commentCount,
        Long viewCount,
        PostType postType) {}
