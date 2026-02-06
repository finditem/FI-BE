package com.fmi.domain.post.converter.util;

import com.fmi.domain.post.data.ImageType;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.web.dto.response.PostImageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public final class PostImageConverter {
    public static List<PostImage> createPostImageList(List<String> images, Post post) {
        if (Objects.isNull(images) || images.isEmpty()) {
            return new ArrayList<>();
        }

        return IntStream.range(0, images.size())
                .mapToObj(i -> {
                    ImageType imageType = (i == 0) ? ImageType.THUMBNAIL : ImageType.NORMAL;
                    return createPostImage(images.get(i), imageType, post);
                }).toList();
    }

    public static PostImage createPostImage(String url, ImageType imageType, Post post) {
        return PostImage.create(url, imageType, post);
    }

    public static List<PostImageResponse> toResponseList(List<PostImage> imageList) {
        return imageList.stream()
                .map(PostImageConverter::toResponse)
                .toList();
    }

    public static PostImageResponse toResponse(PostImage postImage) {
        return new PostImageResponse(postImage.getId(), postImage.getImgUrl(), postImage.getImageType());
    }

    private PostImageConverter() {
    }


}
