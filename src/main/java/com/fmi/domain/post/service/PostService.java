package com.fmi.domain.post.service;

import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.converter.util.PostConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.internal.PostValidator;
import com.fmi.domain.post.web.dto.request.PostCreateRequest;
import com.fmi.domain.post.web.dto.request.PostRadiusUpdateRequest;
import com.fmi.domain.post.web.dto.request.PostStatusUpdateRequest;
import com.fmi.domain.post.web.dto.request.PostUpdateRequest;
import com.fmi.domain.post.web.dto.response.PostCreateResponse;
import com.fmi.domain.post.web.dto.response.PostUpdateResponse;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.temperature.data.TemperatureActivityType;
import com.fmi.domain.temperature.service.TemperatureService;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final PostFavoriteRepository postFavoriteRepository;
    private final PostImageService postImageService;
    private final ChatRoomRepository chatRoomRepository;
    private final PostUpdateRateLimiter postUpdateRateLimiter;
    private final PostTranslationService postTranslationService;
    private final PostValidator postValidator;
    private final TemperatureService temperatureService;
    private final UserRepository userRepository;
    private final PostStatisticsRecorder postStatisticsRecorder;
    private final Clock clock;

    // 게시글 생성
    @Transactional
    public PostCreateResponse createPost(
            PostCreateRequest request, UserDetails userDetails, List<MultipartFile> images) {
        postValidator.validateDate(request.date());

        User user = userRepository
                .findActiveByEmailForUpdate(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        LocalDateTime occurredAt = LocalDateTime.now(clock);
        Post post = PostConverter.toEntity(request, user);

        Post savePost = postRepository.save(post);

        if (savePost.getPostType() == PostType.FOUND) {
            temperatureService.record(
                    TemperatureActivityType.FOUND_POST, user.getId(), savePost.getId(), savePost.getCreatedAt());
        }

        postImageService.createPostImageAtS3AndDB(images, savePost);

        notificationService.notifyCategoriesForPost(post);
        if (!post.isTemporarySave()) {
            postStatisticsRecorder.recordPublication(post.getPostType(), occurredAt);
        }
        return PostConverter.toCreateResponse(post);
    }

    // 게시글 수정
    @Transactional
    public PostUpdateResponse updatePost(
            Long postId, PostUpdateRequest request, UserDetails userDetails, List<MultipartFile> images) {
        postValidator.validateDate(request.date());

        userRepository
                .findActiveByEmailForUpdate(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        Post post = postRepository
                .findActiveByIdForUpdate(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));
        checkPostAccessDenied(post, userDetails.getUsername());
        postValidator.validatePublicationTransition(post, request.temporarySave());
        boolean wasTemporary = post.isTemporarySave();
        LocalDateTime occurredAt = LocalDateTime.now(clock);

        postUpdateRateLimiter.checkAndRecord(postId);

        PostStatus previousStatus = post.getPostStatus();

        post.update(
                request.title(),
                request.postStatus(),
                request.date(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.content(),
                request.temporarySave(),
                request.radius(),
                request.category());

        postTranslationService.invalidateStaleTranslations(post);

        postImageService.deleteImagesNotIn(post.getId(), request.keepImageIdList());

        List<PostImage> newlySaved = List.of();
        if (images != null && !images.isEmpty()) {
            newlySaved = postImageService.createPostImageNormalAtS3AndDB(images, post);
        }

        postImageService.applyThumbnailOnUpdate(post, request.thumbnailImageId(), newlySaved);

        if (!previousStatus.equals(post.getPostStatus())) {
            notifyFavoriteUsers(post, post.getPostStatus());
        }

        if (wasTemporary && !post.isTemporarySave()) {
            postStatisticsRecorder.recordPublication(post.getPostType(), occurredAt);
        }
        return PostConverter.toUpdateResponse(post);
    }

    @Transactional
    public void deletePost(Long postId, UserDetails userDetails) {
        userRepository
                .findActiveByEmailForUpdate(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        Post post = postRepository
                .findActiveByIdForUpdate(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));
        checkPostAccessDenied(post, userDetails.getUsername());

        String thumbnailImageUrl = postImageService.findThumbnailImageUrl(post);
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByPost(post);
        chatRooms.forEach(room -> room.snapshotFromPost(post, thumbnailImageUrl));

        LocalDateTime occurredAt = LocalDateTime.now(clock);
        post.softDelete(occurredAt);
        postImageService.deleteAllImageByPost(post);
        postFavoriteRepository.deleteAllByPostId(postId);
        if (!post.isTemporarySave()) postStatisticsRecorder.recordRemoval(1, occurredAt);
    }

    @Transactional
    public void softDeleteAllByUser(User user) {
        LocalDateTime occurredAt = LocalDateTime.now(clock);
        softDeleteAllByUser(user, occurredAt);
    }

    @Transactional
    public void softDeleteAllByUser(User user, LocalDateTime occurredAt) {
        // 탈퇴 흐름은 이미 회원 잠금을 소유하며, 탈퇴 직후 행도 다시 잠글 수 있습니다.
        userRepository
                .findByIdForUpdate(user.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        List<Post> posts = postRepository.findActiveByUserForUpdate(user);
        long removedCount = 0;
        for (Post post : posts) {
            if (!post.isTemporarySave()) removedCount++;
            post.softDelete(occurredAt);
        }
        postStatisticsRecorder.recordRemoval(removedCount, occurredAt);
    }

    @Transactional
    public void updateRadius(Long postId, PostRadiusUpdateRequest request, UserDetails userDetails) {
        userRepository
                .findActiveByEmailForUpdate(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        Post post = postRepository
                .findActiveByIdForUpdate(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));
        checkPostAccessDenied(post, userDetails.getUsername());
        post.updateRadius(request.radius());
    }

    private void checkPostAccessDenied(Post post, String userEmail) {
        if (!post.getUser().getEmail().equals(userEmail)) {
            throw new GeneralException(ErrorStatus._POST_ACCESS_DENIED);
        }
    }

    // 즐찾 알림
    private void notifyFavoriteUsers(Post post, PostStatus newStatus) {

        List<User> favoriteUserList = postFavoriteRepository.findUsersByPost(post);

        if (favoriteUserList.isEmpty()) return;

        String title = "즐겨찾기 한 게시글의 상태가 변경되었어요.";
        String message = post.getTitle();

        Long postOwnerId = post.getUser().getId();

        for (User user : favoriteUserList) {

            if (user.getId().equals(postOwnerId)) continue;

            notificationService.createNotification(
                    user, NotificationType.FAVORITE, title, message, ReferenceType.POST, post.getId());
            log.info("알림 생성: userId={}, postId={}, newStatus={}", user.getId(), post.getId(), newStatus);
        }
    }

    @Transactional
    public void updatePostStatus(Long postId, PostStatusUpdateRequest request, UserDetails userDetails) {
        User user = userRepository
                .findActiveByEmailForUpdate(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        Post post = postRepository
                .findActiveByIdForUpdate(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        checkPostAccessDenied(post, user.getEmail());

        if (Objects.equals(request.postStatus(), PostStatus.FOUND)) {
            notifyFavoriteUsers(post, PostStatus.FOUND);
        }

        post.updatePostStatus(request.postStatus());
    }

    @Transactional
    public void markAsFound(Post post) {
        notifyFavoriteUsers(post, PostStatus.FOUND);

        post.updatePostStatus(PostStatus.FOUND);
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
    //    public FilterResponse getPostsByFilter(PostFilterDto dto, Pageable pageable, Long cursorId, UserDetails
    // userDetails) {
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
