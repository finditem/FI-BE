package com.fmi.domain.post.post.web.dto.response;

import com.fmi.domain.post.post.data.enums.PostType;

public record PostShareResponse(
        String title,
        String summary,
        String thumbnailUrl,
        String address,
        Long likeCount,
        Long commentCount,
        Long viewCount,
        PostType postType) {}
