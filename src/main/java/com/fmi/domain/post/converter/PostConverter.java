package com.fmi.domain.post.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.response.PostListResponse;
import com.fmi.domain.post.response.PostResponse;
import com.fmi.domain.post.response.PostShareResponse;
import com.fmi.domain.post.web.dto.CreatePostDto;
import com.fmi.domain.post.web.dto.TemporaryPostDto;
import com.fmi.domain.post.web.dto.UpdatePostDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PostConverter {


    private static final String BASE_URL = "https://fmi.com/post/";

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
                .build();
    }

    public PostListResponse toPostListResponse(Post post) {

        String thumbnailUrl = post.getImages().isEmpty() ? null : post.getImages().get(0).getImgUrl();

        String summary = post.getContent() != null
                ? (post.getContent().length() > 20 ? post.getContent().substring(0, 20) + "..." : post.getContent())
                : null;

        return PostListResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .summary(summary)
                .thumbnailUrl(thumbnailUrl)
                .address(post.getAddress())
                .itemStatus(post.getItemStatus())
                .postType(post.getPostType())
                .createdAt(post.getCreatedAt())
                .build();
    }


    public PostShareResponse toShareResponse(Post post) {

        String thumbnailUrl = post.getImages().isEmpty() ? null : post.getImages().get(0).getImgUrl();

        String summary = post.getContent() != null
                ? (post.getContent().length() > 20 ? post.getContent().substring(0, 20) + "..." : post.getContent())
                : null;

        return PostShareResponse.builder()
                .title(post.getTitle())
                .summary(summary)
                .image(thumbnailUrl)
                .url(BASE_URL+post.getId())
                .build();
    }
}
