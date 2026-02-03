package com.fmi.domain.post.web.dto.response;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import com.fmi.domain.user.web.dto.response.UserPostResponse;

import java.time.LocalDateTime;
import java.util.List;

public record PostGetResponse(
        Long id,
        String title,
        String content,
        String address,
        double latitude,
        double longitude,
        PostType postType,
        PostStatus postStatus,
        Radius radius,
        Category category,
        Long favoriteCount,
        boolean favoriteStatus,
        Long viewCount,
        boolean isNew,
        boolean isHot,
        LocalDateTime createdAt,
        List<PostImageResponse> imageResponseList,
        UserPostResponse postUserInformation) {
}
