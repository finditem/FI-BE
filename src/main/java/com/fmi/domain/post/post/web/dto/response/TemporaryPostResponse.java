package com.fmi.domain.post.post.web.dto.response;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.data.enums.Radius;
import java.time.LocalDateTime;
import java.util.List;

public record TemporaryPostResponse(
        Long postId,
        PostType postType,
        Category category,
        Radius radius,
        LocalDateTime date,
        String title,
        String address,
        Double latitude,
        Double longitude,
        String content,
        List<PostImageResponse> images,
        LocalDateTime updatedAt,
        LocalDateTime createdAt) {}
