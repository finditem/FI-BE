package com.fmi.domain.post.service;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.converter.PostConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.repository.PostImageRepository;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.response.PostListResponse;
import com.fmi.domain.post.response.PostResponse;
import com.fmi.domain.post.web.dto.CreatePostDto;
import com.fmi.domain.post.web.dto.TemporaryPostDto;
import com.fmi.domain.post.web.dto.UpdatePostDto;
import com.fmi.global.service.S3Service;
import com.fmi.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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


        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        postConverter.updatePostFromDto(post, request);

        if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {

            List<PostImage> oldImages = postImageRepository.findByPost(post);

            List<PostImage> imagesToDelete = oldImages.stream()
                    .filter(img -> request.getDeleteImageIds().contains(img.getId()))
                    .toList();

            if (!imagesToDelete.isEmpty()) {

                List<String> urlsToDelete = imagesToDelete.stream()
                        .map(PostImage::getImgUrl)
                        .toList();

                s3Service.delete(urlsToDelete);
                postImageRepository.deleteAll(imagesToDelete);
            }
        }

        if (images != null && !images.isEmpty()) {
            List<String> s3Urls = s3Service.upload(images);
            List<PostImage> newImages = postConverter.toPostImageEntities(post, s3Urls);
            postImageRepository.saveAll(newImages);
            post.getImages().addAll(newImages);//추가
        }

        return postConverter.toPostResponse(post);
    }

    @Transactional
    public Post deletePost(Long postId, UserDetails userDetails){

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        List<String> s3Urls = post.getImages().stream()
                .map(PostImage::getImgUrl)
                .toList();

        s3Service.delete(s3Urls);
        postRepository.delete(post);

        return post;
    }

    @Transactional(readOnly = true)
    public List<PostListResponse> getAllPosts(Type type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findByTemporarySaveFalseAndPostType(type, pageable);

        return postPage.stream()
                .map(postConverter::toPostListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        return postConverter.toPostResponse(post);
    }

    @Transactional
    public void saveTemporaryPost(TemporaryPostDto request, UserDetails userDetails, List<MultipartFile> images) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        Optional<Post> existingTempPost = postRepository.findByUserAndTemporarySaveTrue(user);

        if (existingTempPost.isEmpty()) {
            Post newTempPost = postConverter.toTemporaryPostEntity(request, user);

            if (images != null && !images.isEmpty()) {
                List<String> s3Urls = s3Service.upload(images);
                List<PostImage> postImages = postConverter.toPostImageEntities(newTempPost, s3Urls);
                postImageRepository.saveAll(postImages);
            }

            postRepository.save(newTempPost);

        }
        else {
            Post tempPost = existingTempPost.get();
            postConverter.temporaryPostFromDto(tempPost, request);
            tempPost.setTemporarySave(true);

            if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
                List<PostImage> oldImages = postImageRepository.findByPost(tempPost);

                List<PostImage> imagesToDelete = oldImages.stream()
                        .filter(img -> request.getDeleteImageIds().contains(img.getId()))
                        .toList();

                if (!imagesToDelete.isEmpty()) {
                    List<String> urlsToDelete = imagesToDelete.stream()
                            .map(PostImage::getImgUrl)
                            .toList();
                    s3Service.delete(urlsToDelete);

                    postImageRepository.deleteAll(imagesToDelete);
                }
            }

            if (images != null && !images.isEmpty()) {
                List<String> s3Urls = s3Service.upload(images);
                List<PostImage> newImages = postConverter.toPostImageEntities(tempPost, s3Urls);
                postImageRepository.saveAll(newImages);
            }
        }
    }

    @Transactional(readOnly = true)
    public PostResponse getTemporaryPost(UserDetails userDetails){

        Post post = postRepository.findByUserEmailAndTemporarySaveTrue(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자의 임시 게시글을 찾을 수 없습니다."));

        return postConverter.toPostResponse(post);
    }

    @Transactional
    public Post deleteTemporaryPost(UserDetails userDetails){

        Post post = postRepository.findByUserEmailAndTemporarySaveTrue(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자의 임시 게시글을 찾을 수 없습니다."));

        List<String> s3Urls = post.getImages().stream()
                .map(PostImage::getImgUrl)
                .toList();

        if (!s3Urls.isEmpty()) {
            s3Service.delete(s3Urls);
        }

        postRepository.delete(post);

        return post;
    }


}
