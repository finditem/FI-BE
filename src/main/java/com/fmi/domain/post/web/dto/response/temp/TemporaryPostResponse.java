package com.fmi.domain.post.web.dto.response.temp;

import com.fmi.domain.post.data.enums.Category;
import com.fmi.domain.post.data.enums.PostType;
import com.fmi.domain.post.data.enums.Radius;
import com.fmi.domain.post.web.dto.response.image.PostImageResponse;
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
