package com.fmi.domain.post.service;

import com.fmi.domain.User;
import com.fmi.domain.post.converter.PostConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.repository.PostImageRepository;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.response.PostResponse;
import com.fmi.domain.post.web.dto.CreatePostDto;
import com.fmi.domain.post.web.dto.UpdatePostDto;
import com.fmi.global.service.S3Service;
import com.fmi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final S3Service s3Service;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final PostConverter postConverter;

    @Transactional
    public PostResponse createPost(CreatePostDto request, UserDetails userDetails, List<MultipartFile> images) {

        String email= userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다."));

        List<String> s3Urls = (images == null || images.isEmpty())
                ? new ArrayList<>()
                : s3Service.upload(images);

        Post post = postConverter.toPostEntity(request, user);
        postRepository.save(post);

        List<PostImage> postImages = postConverter.toPostImageEntities(post, s3Urls);
        if (!postImages.isEmpty()) {
            postImageRepository.saveAll(postImages);
        }

        return postConverter.toPostResponse(post);
    }

    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostDto request, UserDetails userDetails, List<MultipartFile> images) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        postConverter.updatePostFromDto(post, request);

        if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {

            List<PostImage> oldImages = postImageRepository.findByPost(post);

            List<PostImage> imagesToDelete = oldImages.stream()
                    .filter(img -> request.getDeleteImageIds().contains(img.getId()))
                    .toList();

            if (!imagesToDelete.isEmpty()) {
                postImageRepository.deleteAll(imagesToDelete);
            }
        }

        if (images != null && !images.isEmpty()) {
            List<String> s3Urls = s3Service.upload(images);
            List<PostImage> newImages = postConverter.toPostImageEntities(post, s3Urls);
            if (!newImages.isEmpty()) {
                postImageRepository.saveAll(newImages);
            }
        }

        return postConverter.toPostResponse(post);
    }

    @Transactional
    public Post deletePost(Long postId, UserDetails userDetails){

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new RuntimeException("게시글을 찾을 수 없습니다."));

        if(!post.getUser().getUserId().equals(user.getUserId())){
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);

        return post;
    }


}
