package com.fmi.domain.post.converter;

import com.fmi.domain.User;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.response.PostResponse;
import com.fmi.domain.post.web.dto.CreatePostDto;
import com.fmi.domain.post.web.dto.UpdatePostDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PostConverter {

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
                .pContent(request.getContent())
                .viewCnt(0)
                .date(request.getDate())
                .radius(request.getRadius())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .temporarySave(request.isTemporarySave())
                .build();
    }

    public List<PostImage> toPostImageEntities(Post post, List<String> imageUrls) {
        List<PostImage> postImages = new ArrayList<>();
        int order = 1;
        for (String url : imageUrls) {
            postImages.add(PostImage.builder()
                    .post(post)
                    .img_url(url)
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

        post.setTemporarySave(dto.isTemporarySave());
        post.setUpdatedAt(LocalDateTime.now());
    }


    public PostResponse toPostResponse(Post post) {
        List<String> imageUrls = post.getImages() != null ?
                post.getImages().stream()
                        .map(PostImage::getImg_url)
                        .toList() :
                List.of();

        return PostResponse.builder()
                .postId(post.getPostId())
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
}
