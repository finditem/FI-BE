package com.fmi.domain.post.service;

import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.web.dto.request.TemporaryPostCreateRequest;
import com.fmi.domain.post.web.dto.response.image.PostImageResponse;
import com.fmi.domain.post.web.dto.response.temp.TemporaryPostResponse;
import com.fmi.domain.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.UserQueryService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TemporaryPostService {
    private final PostRepository postRepository;
    private final UserQueryService userQueryService;
    private final PostImageService postImageService;

    @Transactional
    public void create(TemporaryPostCreateRequest request, UserDetails userDetails, List<MultipartFile> newImageList) {
        User user = userQueryService.findUser(userDetails.getUsername());

        Post post = postRepository
                .findByUserAndTemporarySaveTrueAndDeletedFalse(user)
                .orElseGet(() -> Post.createTemporary(user));

        post.updateTemporary(
                request.postType(),
                request.category(),
                request.radius(),
                request.date(),
                request.title(),
                request.address(),
                request.content(),
                request.latitude(),
                request.longitude());

        boolean isNewTemp = Objects.isNull(post.getId());

        Post savePost = postRepository.save(post);

        if (!isNewTemp) {
            postImageService.deleteImagesNotIn(savePost.getId(), request.keepImageIdList());
        }

        if (Objects.nonNull(newImageList) && !newImageList.isEmpty()) {
            postImageService.createPostImageNormalAtS3AndDB(newImageList, savePost);
        }
    }

    @Transactional(readOnly = true)
    public TemporaryPostResponse getMyTemporaryPost(UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());
        Post post = postRepository
                .findByUserAndTemporarySaveTrueAndDeletedFalse(user)
                .orElse(null);

        if (Objects.isNull(post)) {
            return null;
        }

        List<PostImage> imageList = postImageService.findAllByPost(post);

        List<PostImageResponse> images = imageList.stream()
                .map(image -> new PostImageResponse(image.getId(), image.getImgUrl(), image.getImageType()))
                .toList();

        return new TemporaryPostResponse(
                post.getId(),
                post.getPostType(),
                post.getCategory(),
                post.getRadius(),
                post.getDate(),
                post.getTitle(),
                post.getAddress(),
                post.getLatitude(),
                post.getLongitude(),
                post.getContent(),
                images,
                post.getUpdatedAt(),
                post.getCreatedAt());
    }

    @Transactional
    public void deleteTemporaryPost(UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());
        Post post = postRepository
                .findByUserAndTemporarySaveTrueAndDeletedFalse(user)
                .orElseThrow(() -> new GeneralException(ErrorStatus._TEMP_POST_NOT_FOUND));

        postImageService.deleteAllImageByPost(post);
        postRepository.delete(post);
    }
}
