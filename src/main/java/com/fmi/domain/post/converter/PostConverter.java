package com.fmi.domain.post.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.response.*;
import com.fmi.domain.post.web.dto.CreatePostDto;
import com.fmi.domain.post.web.dto.TemporaryPostDto;
import com.fmi.domain.post.web.dto.UpdatePostDto;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PostConverter {

    private String createThumbnail(Post post) {
        if (post.getImages() == null || post.getImages().isEmpty()) {
            return null;
        }
        return post.getImages().get(0).getImgUrl();
    }
    private String createSummary(Post post, int length) {
        if (post.getContent() == null || post.getContent().isEmpty()) return null;
        return post.getContent().length() > length
                ? post.getContent().substring(0, length) + "..."
                : post.getContent();
    }


    public Post toPostEntity(CreatePostDto request, User user) {
        return Post.builder()
                .user(user)
                .title(request.getTitle())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .itemStatus(request.getItemStatus())
                .postType(request.getPostType())
                .content(request.getContent())
                .viewCnt(0)
                .date(request.getDate())
                .radius(request.getRadius())
                .category(request.getCategory())
                .temporarySave(request.isTemporarySave())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Post toTemporaryPostEntity(TemporaryPostDto request, User user) {
        return Post.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .temporarySave(true)
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .itemStatus(request.getItemStatus())
                .postType(request.getPostType())
                .radius(request.getRadius())
                .viewCnt(0)
                .date(request.getDate())
                .category(request.getCategory())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public List<PostImage> toPostImageEntities(Post post, List<String> imageUrls) {
        List<PostImage> postImages = new ArrayList<>();

        int maxOrder = 0;
        for (PostImage img : post.getImages()) {
            if (img.getDisplayOrder() > maxOrder) {
                maxOrder = img.getDisplayOrder();
            }
        }
        int order = maxOrder + 1;

        for (String url : imageUrls) {
            postImages.add(PostImage.builder()
                    .post(post)
                    .imgUrl(url)
                    .displayOrder(order++)
                    .build());
        }
        return postImages;
    }


    public void updatePostFromDto(Post post, UpdatePostDto dto) {
        if (dto.getPostType() != null) post.setPostType(dto.getPostType());
        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
        if (dto.getItemStatus() != null) post.setItemStatus(dto.getItemStatus());
        if (dto.getDate() != null) post.setDate(dto.getDate());
        if (dto.getAddress() != null) post.setAddress(dto.getAddress());
        if (dto.getLatitude() != 0) post.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != 0) post.setLongitude(dto.getLongitude());
        if (dto.getRadius() != 0) post.setRadius(dto.getRadius());
        if (dto.getContent() != null) post.setContent(dto.getContent());
        if (dto.getCategory() != null) post.setCategory(dto.getCategory());

        post.setUpdatedAt(LocalDateTime.now());
    }

    public void temporaryPostFromDto(Post post, TemporaryPostDto dto) {
        if (dto.getPostType() != null) post.setPostType(dto.getPostType());
        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
        if (dto.getItemStatus() != null) post.setItemStatus(dto.getItemStatus());
        if (dto.getDate() != null) post.setDate(dto.getDate());
        if (dto.getAddress() != null) post.setAddress(dto.getAddress());
        if (dto.getLatitude() != 0) post.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != 0) post.setLongitude(dto.getLongitude());
        if (dto.getRadius() != 0) post.setRadius(dto.getRadius());
        if (dto.getContent() != null) post.setContent(dto.getContent());
        if (dto.getCategory() != null) post.setCategory(dto.getCategory());

        post.setUpdatedAt(LocalDateTime.now());
    }

    public PostResponse toPostResponse(Post post) {

        List<String> imageUrls = post.getImages() != null ?
                post.getImages().stream()
                        .map(PostImage::getImgUrl)
                        .toList() :
                List.of();

        return PostResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .address(post.getAddress())
                .latitude(post.getLatitude())
                .longitude(post.getLongitude())
                .radius(post.getRadius())
                .imageUrls(imageUrls)
                .itemStatus(post.getItemStatus())
                .postType(post.getPostType())
                .category(post.getCategory())
                .favoriteCount(post.getFavoriteCount())
                .build();
    }

    public PostResponse toPostDetailResponse(Post post, boolean isFavorite, Long viewCount, Long hotPostId) {

        List<String> imageUrls = post.getImages() != null ?
                post.getImages().stream()
                        .map(PostImage::getImgUrl)
                        .toList() :
                List.of();

        return PostResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .address(post.getAddress())
                .latitude(post.getLatitude())
                .longitude(post.getLongitude())
                .radius(post.getRadius())
                .imageUrls(imageUrls)
                .itemStatus(post.getItemStatus())
                .postType(post.getPostType())
                .category(post.getCategory())
                .favoriteCount(post.getFavoriteCount())
                .favoriteStatus(isFavorite)
                .viewCount(viewCount)
                .isNew(post.isNew())
                .isHot(post.getId().equals(hotPostId))
                .build();
    }

    public PostListResponse toPostListResponse(Post post,Long hotPostId) {

        return PostListResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .summary(createSummary(post,20))
                .thumbnailUrl(createThumbnail(post))
                .address(post.getAddress())
                .itemStatus(post.getItemStatus())
                .postType(post.getPostType())
                .favoriteCount(post.getFavoriteCount())
                .category(post.getCategory())
                .createdAt(post.getCreatedAt())
                .isNew(post.isNew())
                .isHot(post.getId().equals(hotPostId))
                .build();
    }


    public PostShareResponse toShareResponse(Post post) {

        return PostShareResponse.builder()
                .title(post.getTitle())
                .summary(createSummary(post,100))
                .thumbnailUrl(createThumbnail(post))
                .build();
    }

    public ViewResponse toViewResponse(long postId, long viewcnt){
        return ViewResponse.builder()
                .postId(postId)
                .viewCount(viewcnt)
                .build();
    }

    public FilterResponse toFilterResponse(Slice<Post> slice, Long hotPostId) {
        List<PostListResponse> postDtos = slice.getContent()
                .stream()
                .map(post -> toPostListResponse(post, hotPostId))
                .toList();

        Long nextCursor = slice.hasNext()
                ? slice.getContent().get(slice.getContent().size() - 1).getId()
                : null;

        return new FilterResponse(postDtos, slice.hasNext(), nextCursor);
    }



}
