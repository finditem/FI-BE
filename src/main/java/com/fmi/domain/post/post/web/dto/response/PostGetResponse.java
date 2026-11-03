package com.fmi.domain.post.post.web.dto.response;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.data.enums.Radius;
import com.fmi.domain.user.user.web.dto.response.UserPostResponse;
import io.swagger.v3.oas.annotations.media.Schema;
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

        @Schema(description = "게시글 등록 일시(Asia/Seoul)", example = "2026-09-14T00:30:00")
        LocalDateTime createdAt,

        @Schema(description = "분실 또는 습득 일시(Asia/Seoul)", example = "2026-09-13T18:20:00")
        LocalDateTime date,

        boolean isMine,
        List<PostImageResponse> imageResponseList,
        UserPostResponse postUserInformation) {}
