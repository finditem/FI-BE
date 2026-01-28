package com.fmi.domain.post.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.response.*;
import com.fmi.domain.post.web.dto.request.PostCreateRequest;
import com.fmi.domain.post.web.dto.response.PostCreateResponse;
import com.fmi.domain.post.web.dto.response.PostGetResponse;
import com.fmi.domain.post.web.dto.response.PostImageResponse;
import com.fmi.domain.post.web.dto.response.PostUpdateResponse;

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
                request.temporarySave(),
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

    public static PostGetResponse toGetResponse(Post post, boolean isFavorite, long viewCount, long chatRoomCount, boolean isNew, boolean isHot, long favoriteCount, long userPostCount, List<PostImageResponse> imageList) {
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
                chatRoomCount,
                userPostCount,
                imageList
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


//    public PostShareResponse toShareResponse(Post post) {
//
//        return PostShareResponse.builder()
//                .title(post.getTitle())
//                .summary(createSummary(post, 100))
//                .thumbnailUrl(createThumbnail(post))
//                .build();
//    }



    //TODO 이거 물어봐서 해보기
    public PostListResponse toPostListResponse(Post post, Long hotPostId, Long viewCount, boolean isFavorite) {

        return PostListResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
//                .summary(createSummary(post, 20))
                .summary(null)
//                .thumbnailUrl(createThumbnail(post))
                .thumbnailUrl(null)
                .address(post.getAddress())
//                .itemStatus(post.getItemStatus())
                .itemStatus(null)
//                .postType(post.getPostType())
                .postType(null)
//                .favoriteCount(post.getFavoriteCount())
                .favoriteCount(null)
                .category(post.getCategory())
                .createdAt(post.getCreatedAt())
                .isNew(post.isNew())
                .isHot(post.getId().equals(hotPostId))
                .viewCount(viewCount)
                .favoriteStatus(isFavorite)
                .build();
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
