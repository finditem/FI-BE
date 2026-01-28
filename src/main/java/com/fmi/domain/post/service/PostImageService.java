package com.fmi.domain.post.service;

import com.fmi.domain.post.converter.PostImageConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.repository.PostImageRepository;
import com.fmi.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostImageService {
    private final S3Service s3Service;
    private final PostImageRepository postImageRepository;

    @Transactional
    public void createPostImage(List<MultipartFile> imageList, Post post){
        List<PostImage> postImageList = new ArrayList<>();
        if (Objects.nonNull(imageList)) {
            postImageList = PostImageConverter.createPostImageList(s3Service.upload(imageList), post);
        }

        postImageRepository.saveAll(postImageList);
    }

    @Transactional
    public void deleteImageByPost(Post post) {
        List<PostImage> postImageList = postImageRepository.findByPost(post);

        List<String> urlList = postImageList.stream().map(PostImage::getImgUrl).toList();

        s3Service.delete(urlList);

        postImageList.forEach(postImageRepository::delete);
    }
}
