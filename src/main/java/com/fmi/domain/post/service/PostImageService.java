package com.fmi.domain.post.service;

import com.fmi.domain.post.converter.PostImageConverter;
import com.fmi.domain.post.data.ImageType;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.repository.PostImageRepository;
import com.fmi.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostImageService {
    private final S3Service s3Service;
    private final PostImageRepository postImageRepository;

    @Transactional
    public void createPostImageAtS3AndDB(List<MultipartFile> imageList, Post post) {
        List<PostImage> postImageList = new ArrayList<>();
        if (Objects.nonNull(imageList)) {
            postImageList = PostImageConverter.createPostImageList(s3Service.upload(imageList), post);
        }

        postImageRepository.saveAll(postImageList);
    }

    @Transactional
    public void deleteAllImageByPost(Post post) {
        List<PostImage> postImageList = postImageRepository.findByPost(post);

        List<String> urlList = postImageList.stream().map(PostImage::getImgUrl).toList();

        s3Service.delete(urlList);

        postImageRepository.deleteAllByPost(post);
    }

    @Transactional
    public void deleteImageAtS3(List<PostImage> imageList) {
        List<String> imageUrlList = imageList.stream().map(PostImage::getImgUrl).toList();
        s3Service.delete(imageUrlList);
    }

    @Transactional
    public void deleteImageAtDB(List<PostImage> imageList) {
        imageList.forEach(postImageRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PostImage> findAllByPost(Post post) {
        return postImageRepository.findByPost(post);
    }

    @Transactional(readOnly = true)
    public PostImage findThumbnailImage(Post post) {
        return postImageRepository.findByPost_IdAndImageType(post.getId(), ImageType.THUMBNAIL).orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findThumbnailUrlByPostList(List<Post> postList) {
        if (Objects.isNull(postList) || postList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> postIds = postList.stream()
                .map(Post::getId)
                .toList();

        List<PostImage> thumbnails =
                postImageRepository.findThumbnailImagesByPostIds(postIds);

        return thumbnails.stream()
                .collect(Collectors.toMap(
                        pi -> pi.getPost().getId(),   // key: postId
                        PostImage::getImgUrl
                ));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> countImageByPostList(List<Post> postList) {
        if (Objects.isNull(postList) || postList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> postIds = postList.stream()
                .map(Post::getId)
                .toList();

        return postImageRepository.countImagesGroupByPostId(postIds);
    }
}
