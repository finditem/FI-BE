package com.fmi.domain.post.web.dto.request;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;

import java.time.LocalDateTime;
import java.util.List;

public record TemporaryPostCreateRequest(
        PostType postType,
        String title,
        LocalDateTime date,
        String address,
        Double latitude,
        Double longitude,
        String content,
        Radius radius,
        Category category,

        List<Long> keepImageIdList
) {
}
