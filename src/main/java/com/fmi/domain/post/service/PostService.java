package com.fmi.domain.post.service;

import com.fmi.domain.Enum.Status;
import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.post.converter.PostConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.repository.PostImageRepository;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.response.*;
import com.fmi.domain.post.web.dto.CreatePostDto;
import com.fmi.domain.post.web.dto.PostFilterDto;
import com.fmi.domain.post.web.dto.TemporaryPostDto;
import com.fmi.domain.post.web.dto.UpdatePostDto;
import com.fmi.domain.post.web.dto.response.PostListSliceResponse;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import com.fmi.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.fmi.domain.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final S3Service s3Service;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final PostConverter postConverter;
    private final NotificationService notificationService;
    private final PostFavoriteRepository postFavoriteRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChatRoomRepository chatRoomRepository;

// 게시글 생성
    @Transactional
    public PostResponse createPost(CreatePostDto request, UserDetails userDetails, List<MultipartFile> images) {

        String email = userDetails.getUsername();

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
            post.setImages(postImages);
        }

        // 임시 저장이 아닌 경우에만 카테고리 알림 트리거
        if (!post.isTemporarySave()) {
            notificationService.notifyCategoriesForPost(post);
        }
        return postConverter.toPostResponse(post);
    }

    //게시글 수정
    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostDto request, UserDetails userDetails, List<MultipartFile> images) {


        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        Status previousStatus = post.getItemStatus();

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

        if (!previousStatus.equals(post.getItemStatus())) {
            notifyFavoritedUsers(post, post.getItemStatus());
        }

        return postConverter.toPostResponse(post);
    }

    //즐찾 알림
    private void notifyFavoritedUsers(Post post, Status newStatus) {

        List<User> favoritedUsers = postFavoriteRepository.findUsersByPost(post);

        if (favoritedUsers.isEmpty()) return;

        String title = "즐겨찾기한 게시글 상태 변경";
        String message = switch (newStatus) {
            case FOUND -> String.format("[%s] 게시글이 '찾음' 상태로 변경되었습니다.", post.getTitle());
            case SEARCHING -> String.format("[%s] 게시글이 '찾는중' 상태로 변경되었습니다.", post.getTitle());
        };

        Long postOwnerId = post.getUser().getId();

        for (User user : favoritedUsers) {

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

    //게시글 삭제
    @Transactional
    public Post deletePost(Long postId, UserDetails userDetails) {

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

    //게시글 전체 조회
    @Transactional(readOnly = true)
    public PostListSliceResponse getAllPosts(Type type, Long cursor, int size,UserDetails userDetails) {

        Long hotPostId = getHotPostId();
        Pageable pageable = PageRequest.of(0, size, Sort.by("createdAt").descending());

        Slice<Post> postSlice;
        if (cursor == null) {
            postSlice = postRepository.findByTemporarySaveFalseAndPostType(type, pageable);
        } else {
            postSlice = postRepository.findByTemporarySaveFalseAndPostTypeAndIdLessThan(type, cursor, pageable);
        }
        List<Post> posts = postSlice.getContent();
        List<Long> postIds = postSlice.getContent().stream().map(Post::getId).toList();

        Map<Long, Long> viewCounts = getViewCountsFromRedis(postIds);
        Set<Long> favoritePostIds = getFavoritePostIds(userDetails, posts);

        List<PostListResponse> summaries = posts.stream()
                .map(post -> postConverter.toPostListResponse(
                        post,
                        hotPostId,
                        viewCounts.getOrDefault(post.getId(), 0L),
                        favoritePostIds.contains(post.getId())
                ))
                .toList();

        Long nextCursor = postSlice.hasNext()
                ? postSlice.getContent().get(postSlice.getContent().size() - 1).getId()
                : null;

        return new PostListSliceResponse(summaries, nextCursor, postSlice.hasNext());
    }

    // 레디스에 게시글 조회수 가져오기
    public Map<Long, Long> getViewCountsFromRedis(List<Long> postIds) {
        if (postIds.isEmpty()) return Collections.emptyMap();

        List<String> keys = postIds.stream()
                .map(id -> "post:view:count:" + id)
                .toList();

        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);

        Map<Long, Long> viewCountMap = new HashMap<>();
        for (int i = 0; i < postIds.size(); i++) {
            String val = values.get(i);
            viewCountMap.put(postIds.get(i), val != null ? Long.parseLong(val) : 0L);
        }
        return viewCountMap;
    }

    // 게시글 단일 조회
    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, UserDetails userDetails) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        Long hotPostId = getHotPostId();
        Long chatRoomCount= chatRoomRepository.countByPostId(postId);
        Long userPostCount = postRepository.countByUserAndTemporarySaveFalse(post.getUser());

        boolean isFavorite = false;

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

            PostFavorite favorite = postFavoriteRepository.findByUserAndPost(user, post)
                    .orElse(null);

            isFavorite = favorite != null && favorite.isFavorite();
        }
        Long viewCount = increaseViewCount(postId, userDetails);

        return postConverter.toPostDetailResponse(post, isFavorite, viewCount, hotPostId, chatRoomCount,userPostCount);
    }

    private Long increaseViewCount(Long postId, UserDetails userDetails) {

        String viewCountKey = "post:view:count:" + postId;

        if (userDetails == null) {
            String currentCount = stringRedisTemplate.opsForValue().get(viewCountKey);
            return currentCount != null ? Long.parseLong(currentCount) : 0L;
        }

        String email = userDetails.getUsername();
        String userCheckKey = "post:view:lock:" + postId + ":" + email;
        String changedIdsKey = "post:changed:ids";

        Boolean isFirstView = stringRedisTemplate.opsForValue()
                .setIfAbsent(userCheckKey, "y", Duration.ofMinutes(5));

        if (Boolean.TRUE.equals(isFirstView)) {
            Long updatedCount = stringRedisTemplate.opsForValue().increment(viewCountKey);
            stringRedisTemplate.opsForSet().add(changedIdsKey, postId.toString());

            return updatedCount;
        }

        String currentCount = stringRedisTemplate.opsForValue().get(viewCountKey);
        return currentCount != null ? Long.parseLong(currentCount) : 0L;
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

        } else {
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
    public PostResponse getTemporaryPost(UserDetails userDetails) {

        Post post = postRepository.findByUserEmailAndTemporarySaveTrue(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자의 임시 게시글을 찾을 수 없습니다."));

        return postConverter.toPostResponse(post);
    }

    @Transactional
    public Post deleteTemporaryPost(UserDetails userDetails) {

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

    @Transactional
    public PostShareResponse getSharePost(Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글 없음"));

        return postConverter.toShareResponse(post);
    }


    @Transactional(readOnly = true)
    public FilterResponse getPostsByFilter(PostFilterDto dto, Pageable pageable, Long cursorId, UserDetails userDetails) {
        Long hotPostId = getHotPostId();
        Slice<Post> slice = postRepository.findPostsByFilters(dto, pageable, cursorId);

        List<Post> posts = slice.getContent();
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        Map<Long, Long> viewCounts = getViewCountsFromRedis(postIds);

        Set<Long> favoritePostIds = getFavoritePostIds(userDetails, posts);

        return postConverter.toFilterResponse(slice, hotPostId, viewCounts, favoritePostIds);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void syncViewCountsToDb() {
        ScanOptions options = ScanOptions.scanOptions().match("post:view:count:*").build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {

            Map<Long, Long> viewCountMap = new HashMap<>();

            while (cursor.hasNext()) {
                String key = cursor.next();
                Long postId = Long.parseLong(key.substring("post:view:count:".length()));
                Long count = Optional.ofNullable(stringRedisTemplate.opsForValue().get(key))
                        .map(Long::parseLong)
                        .orElse(0L);

                if (count > 0) {
                    viewCountMap.put(postId, count);
                }
                stringRedisTemplate.delete(key);

            }

            if (!viewCountMap.isEmpty()) {
                postRepository.batchIncrementViewCounts(viewCountMap);
            }


        }
    }

    public Long getHotPostId() {
        return postRepository.findHotPost(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(Post::getId)
                .orElse(null);
    }

    private Set<Long> getFavoritePostIds(UserDetails userDetails, List<Post> posts) {

        if (userDetails == null || posts.isEmpty()) {
            return Collections.emptySet();
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        return postFavoriteRepository.findPostIdsByUserAndPostIn(user, posts);
    }
}
