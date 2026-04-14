package com.fmi.domain.post.converter.util;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.web.dto.request.PostCreateRequest;
import com.fmi.domain.post.web.dto.response.*;
import com.fmi.domain.post.web.dto.response.image.PostImageResponse;
import com.fmi.domain.user.web.dto.response.UserPostResponse;

import java.util.List;

public final class PostConverter {

    public static Post toEntity(PostCreateRequest request, User user) {
        return Post.create(
                request.title(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.postType(),
                request.category(),
                request.content(),
                false,
                request.date(),
                request.radius(),
                user);
    }

    public static PostCreateResponse toCreateResponse(Post post) {
        return new PostCreateResponse(post.getId());
    }

    public static PostUpdateResponse toUpdateResponse(Post post) {
        return new PostUpdateResponse(post.getId());
    }

    public static PostGetResponse toGetResponse(Post post, boolean isFavorite, long viewCount, boolean isNew, boolean isHot, long favoriteCount, boolean isMine, List<PostImageResponse> imageList, UserPostResponse userPostResponse) {
        return new PostGetResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAddress(),
                post.getLatitude(),
                post.getLongitude(),
                post.getPostType(),
                post.getPostStatus(),
                post.getRadius(),
                post.getCategory(),
                favoriteCount,
                isFavorite,
                viewCount,
                isNew,
                isHot,
                post.getCreatedAt(),
                isMine,
                imageList,
                userPostResponse
        );
    }

    public static PostBriefResponse toPostBriefResponse(Post post, boolean isFavorite, String thumbnailImageUrl, Long favoriteCount, Integer imageCount) {
        return toPostBriefResponse(post, isFavorite, thumbnailImageUrl, favoriteCount, imageCount, false);
    }

    public static PostBriefResponse toPostBriefResponse(Post post, boolean isFavorite, String thumbnailImageUrl, Long favoriteCount, Integer imageCount, boolean isHot) {
        return new PostBriefResponse(
                post.getId(),
                post.getTitle(),
                post.makeSummary(),
                thumbnailImageUrl,
                post.getAddress(),
                post.getPostStatus(),
                post.getPostType(),
                post.getCategory(),
                favoriteCount,
                isFavorite,
                post.getViewCount(),
                post.isNew(),
                isHot,
                post.getCreatedAt(),
                imageCount
        );
    }


//    public Post toTemporaryPostEntity(TemporaryPostDto request, User user) {
//        return Post.builder()
//                .user(user)
//                .title(request.getTitle())
//                .content(request.getContent())
//                .temporarySave(true)
//                .address(request.getAddress())
//                .latitude(request.getLatitude())
//                .longitude(request.getLongitude())
//                .itemStatus(request.getItemStatus())
//                .postType(request.getPostType())
//                .radius(request.getRadius())
//                .viewCnt(0)
//                .date(request.getDate())
//                .category(request.getCategory())
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//    }


//    public void updatePostFromDto(Post post, UpdatePostDto dto) {
//        if (dto.getPostType() != null) post.setPostType(dto.getPostType());
//        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
//        if (dto.getItemStatus() != null) post.setItemStatus(dto.getItemStatus());
//        if (dto.getDate() != null) post.setDate(dto.getDate());
//        if (dto.getAddress() != null) post.setAddress(dto.getAddress());
//        if (dto.getLatitude() != 0) post.setLatitude(dto.getLatitude());
//        if (dto.getLongitude() != 0) post.setLongitude(dto.getLongitude());
//        if (dto.getRadius() != 0) post.setRadius(dto.getRadius());
//        if (dto.getContent() != null) post.setContent(dto.getContent());
//        if (dto.getCategory() != null) post.setCategory(dto.getCategory());
//
//        post.setUpdatedAt(LocalDateTime.now());
//    }

//    public void temporaryPostFromDto(Post post, TemporaryPostDto dto) {
//        if (dto.getPostType() != null) post.setPostType(dto.getPostType());
//        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
//        if (dto.getItemStatus() != null) post.setItemStatus(dto.getItemStatus());
//        if (dto.getDate() != null) post.setDate(dto.getDate());
//        if (dto.getAddress() != null) post.setAddress(dto.getAddress());
//        if (dto.getLatitude() != 0) post.setLatitude(dto.getLatitude());
//        if (dto.getLongitude() != 0) post.setLongitude(dto.getLongitude());
//        if (dto.getRadius() != 0) post.setRadius(dto.getRadius());
//        if (dto.getContent() != null) post.setContent(dto.getContent());
//        if (dto.getCategory() != null) post.setCategory(dto.getCategory());
//
//        post.setUpdatedAt(LocalDateTime.now());
//    }


    public static PostShareResponse toShareResponse(Post post, String thumbnailUrl, Long likeCount, Long commentCount) {
        return new PostShareResponse(
                post.getTitle(),
                post.makeSummary(),
                thumbnailUrl,
                post.getAddress(),
                likeCount,
                commentCount,
                post.getViewCount(),
                post.getPostType()
        );
    }


//    public FilterResponse toFilterResponse(Slice<Post> slice,
//                                           Long hotPostId,
//                                           Map<Long, Long> viewCounts,
//                                           Set<Long> favoritePostIds) {
//
//        List<PostListResponse> postDtos = slice.getContent()
//                .stream()
//                .map(post -> toPostListResponse(
//                        post,
//                        hotPostId,
//                        viewCounts.getOrDefault(post.getId(), 0L), // Redis 값 적용
//                        favoritePostIds.contains(post.getId())    // 즐겨찾기 상태 적용
//                ))
//                .toList();
//
//        Long nextCursor = slice.hasNext()
//                ? slice.getContent().get(slice.getContent().size() - 1).getId()
//                : null;
//
//        return new FilterResponse(postDtos, slice.hasNext(), nextCursor);
//    }

    private PostConverter() {
    }


}
