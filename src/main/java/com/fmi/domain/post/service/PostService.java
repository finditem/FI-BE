package com.fmi.domain.post.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.post.converter.util.PostConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.web.dto.request.PostCreateRequest;
import com.fmi.domain.post.web.dto.request.PostRadiusUpdateRequest;
import com.fmi.domain.post.web.dto.request.PostStatusUpdateRequest;
import com.fmi.domain.post.web.dto.request.PostUpdateRequest;
import com.fmi.domain.post.web.dto.response.PostCreateResponse;
import com.fmi.domain.post.web.dto.response.PostUpdateResponse;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import com.fmi.domain.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final PostFavoriteRepository postFavoriteRepository;
    private final UserQueryService userQueryService;
    private final PostImageService postImageService;
    private final PostQueryService postQueryService;

    // 게시글 생성
    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, UserDetails userDetails, List<MultipartFile> images) {
        User user = userQueryService.findUser(userDetails.getUsername());

        boolean fromTemp = Objects.nonNull(request.tempPostId());

        Post post;
        if (fromTemp) {
            post = postRepository.findById(request.tempPostId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._TEMP_POST_NOT_FOUND));

            checkPostAccessDenied(post, userDetails.getUsername());

            if (!post.isTemporarySave()) {
                throw new GeneralException(ErrorStatus._TEMP_POST_NOT_FOUND);
            }

            post.update(
                    request.postType(),
                    request.title(),
                    PostStatus.SEARCHING,
                    request.date(),
                    request.address(),
                    request.latitude(),
                    request.longitude(),
                    request.content(),
                    false,
                    request.radius(),
                    request.category()
            );

            postImageService.deleteImagesNotIn(post.getId(), request.keepImageIdList());
        } else {
            post = PostConverter.toEntity(request, user);
        }

        post = postRepository.save(post);

        postImageService.applyThumbNail(images, post, request.thumbnailImageId(), request.keepImageIdList());

        notificationService.notifyCategoriesForPost(post);

        return PostConverter.toCreateResponse(post);
    }

    // 게시글 수정
    @Transactional
    public PostUpdateResponse updatePost(Long postId, PostUpdateRequest request, UserDetails userDetails, List<MultipartFile> images) {
        Post post = postQueryService.findById(postId);

        checkPostAccessDenied(post, userDetails.getUsername());

        PostStatus previousStatus = post.getPostStatus();

        post.update(
                request.postType(),
                request.title(),
                request.postStatus(),
                request.date(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.content(),
                request.temporarySave(),
                request.radius(),
                request.category()
        );


        if (Objects.nonNull(request.deleteImageIdList()) && !request.deleteImageIdList().isEmpty()) {

            List<PostImage> oldImages = postImageService.findAllByPost(post);

            List<PostImage> imagesToDelete = oldImages.stream()
                    .filter(img -> request.deleteImageIdList().contains(img.getId()))
                    .toList();

            if (!imagesToDelete.isEmpty()) {
                postImageService.deleteImageAtS3(imagesToDelete);
                postImageService.deleteImageAtDB(imagesToDelete);
            }
        }

        List<PostImage> newlySaved = List.of();
        if (images != null && !images.isEmpty()) {
            newlySaved = postImageService.createPostImageNormalAtS3AndDB(images, post);
        }

        postImageService.applyThumbnailOnUpdate(
                post,
                request.thumbnailImageId(),
                newlySaved
        );


        if (!previousStatus.equals(post.getPostStatus())) {
            notifyFavoriteUsers(post, post.getPostStatus());
        }

        return PostConverter.toUpdateResponse(post);
    }

    @Transactional
    public void deletePost(Long postId, UserDetails userDetails) {
        Post post = postQueryService.findById(postId);
        checkPostAccessDenied(post, userDetails.getUsername());

        postImageService.deleteAllImageByPost(post);
        postFavoriteRepository.deleteAllByPost(post);
        post.softDelete();
    }

    @Transactional
    public void updateRadius(Long postId, PostRadiusUpdateRequest request, UserDetails userDetails) {
        Post post = postQueryService.findById(postId);
        checkPostAccessDenied(post, userDetails.getUsername());
        post.updateRadius(request.radius());
    }

    private void checkPostAccessDenied(Post post, String userEmail) {
        if (!post.getUser().getEmail().equals(userEmail)) {
            throw new GeneralException(ErrorStatus._POST_ACCESS_DENIED);
        }
    }

    //즐찾 알림
    private void notifyFavoriteUsers(Post post, PostStatus newStatus) {

        List<User> favoriteUserList = postFavoriteRepository.findUsersByPost(post);

        if (favoriteUserList.isEmpty()) return;

        String title = "즐겨찾기한 게시글 상태 변경";
        String message = switch (newStatus) {
            case FOUND -> String.format("[%s] 게시글이 '찾음' 상태로 변경되었습니다.", post.getTitle());
            case SEARCHING -> String.format("[%s] 게시글이 '찾는중' 상태로 변경되었습니다.", post.getTitle());
        };

        Long postOwnerId = post.getUser().getId();

        for (User user : favoriteUserList) {

            if (user.getId().equals(postOwnerId)) continue;

            notificationService.createNotification(
                    user,
                    NotificationType.FAVORITE,
                    title,
                    message,
                    ReferenceType.POST,
                    post.getId()
            );
            log.info("알림 생성: userId={}, postId={}, newStatus={}", user.getId(), post.getId(), newStatus);
        }
    }

    @Transactional
    public void updatePostStatus(Long postId, PostStatusUpdateRequest request, UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());
        Post post = postQueryService.findById(postId);

        checkPostAccessDenied(post, user.getEmail());

        if (Objects.equals(request.postStatus(), PostStatus.FOUND)) {
            notifyFavoriteUsers(post, PostStatus.FOUND);
        }

        post.updatePostStatus(request.postStatus());
    }

//    @Transactional
//    public void saveTemporaryPost(TemporaryPostDto request, UserDetails userDetails, List<MultipartFile> images) {
//
//        User user = userRepository.findByEmail(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
//
//        Optional<Post> existingTempPost = postRepository.findByUserAndTemporarySaveTrue(user);
//
//        if (existingTempPost.isEmpty()) {
//            Post newTempPost = postConverter.toTemporaryPostEntity(request, user);
//
//            if (images != null && !images.isEmpty()) {
//                List<String> s3Urls = s3Service.upload(images);
//                List<PostImage> postImages = postConverter.toPostImageEntities(newTempPost, s3Urls);
//                postImageRepository.saveAll(postImages);
//            }
//
//            postRepository.save(newTempPost);
//
//        } else {
//            Post tempPost = existingTempPost.get();
//            postConverter.temporaryPostFromDto(tempPost, request);
//            tempPost.setTemporarySave(true);
//
//            if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
//                List<PostImage> oldImages = postImageRepository.findByPost(tempPost);
//
//                List<PostImage> imagesToDelete = oldImages.stream()
//                        .filter(img -> request.getDeleteImageIds().contains(img.getId()))
//                        .toList();
//
//                if (!imagesToDelete.isEmpty()) {
//                    List<String> urlsToDelete = imagesToDelete.stream()
//                            .map(PostImage::getImgUrl)
//                            .toList();
//                    s3Service.delete(urlsToDelete);
//
//                    postImageRepository.deleteAll(imagesToDelete);
//                }
//            }
//
//            if (images != null && !images.isEmpty()) {
//                List<String> s3Urls = s3Service.upload(images);
//                List<PostImage> newImages = postConverter.toPostImageEntities(tempPost, s3Urls);
//                postImageRepository.saveAll(newImages);
//            }
//        }
//    }
//
//    @Transactional(readOnly = true)
//    public PostResponse getTemporaryPost(UserDetails userDetails) {
//
//        Post post = postRepository.findByUserEmailAndTemporarySaveTrue(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("사용자의 임시 게시글을 찾을 수 없습니다."));
//
//        return postConverter.toPostResponse(post);
//    }
//
//    @Transactional
//    public Post deleteTemporaryPost(UserDetails userDetails) {
//
//        Post post = postRepository.findByUserEmailAndTemporarySaveTrue(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("사용자의 임시 게시글을 찾을 수 없습니다."));
//
//        List<String> s3Urls = post.getImages().stream()
//                .map(PostImage::getImgUrl)
//                .toList();
//
//        if (!s3Urls.isEmpty()) {
//            s3Service.delete(s3Urls);
//        }
//
//        postRepository.delete(post);
//
//        return post;
//    }
//
//    @Transactional
//    public PostShareResponse getSharePost(Long postId) {
//
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new RuntimeException("게시글 없음"));
//
//        return postConverter.toShareResponse(post);
//    }
//
//
//    @Transactional(readOnly = true)
//    public FilterResponse getPostsByFilter(PostFilterDto dto, Pageable pageable, Long cursorId, UserDetails userDetails) {
//        Long hotPostId = getHotPostId();
//        Slice<Post> slice = postRepository.findPostsByFilters(dto, pageable, cursorId);
//
//        List<Post> posts = slice.getContent();
//        List<Long> postIds = posts.stream().map(Post::getId).toList();
//
//        Map<Long, Long> viewCounts = getViewCountsFromRedis(postIds);
//
//        Set<Long> favoritePostIds = getFavoritePostIds(userDetails, posts);
//
//        return postConverter.toFilterResponse(slice, hotPostId, viewCounts, favoritePostIds);
//    }

//    @Scheduled(cron = "0 0 * * * *")
//    public void syncViewCountsToDb() {
//        ScanOptions options = ScanOptions.scanOptions().match("post:view:count:*").build();
//        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
//
//            Map<Long, Long> viewCountMap = new HashMap<>();
//
//            while (cursor.hasNext()) {
//                String key = cursor.next();
//                Long postId = Long.parseLong(key.substring("post:view:count:".length()));
//                Long count = Optional.ofNullable(stringRedisTemplate.opsForValue().get(key))
//                        .map(Long::parseLong)
//                        .orElse(0L);
//
//                if (count > 0) {
//                    viewCountMap.put(postId, count);
//                }
//                stringRedisTemplate.delete(key);
//
//            }
//
//            if (!viewCountMap.isEmpty()) {
//                postRepository.batchIncrementViewCounts(viewCountMap);
//            }
//
//
//        }
//    }

//    public Long getHotPostId() {
//        return postRepository.findHotPost(PageRequest.of(0, 1))
//                .stream()
//                .findFirst()
//                .map(Post::getId)
//                .orElse(null);
//    }
}
