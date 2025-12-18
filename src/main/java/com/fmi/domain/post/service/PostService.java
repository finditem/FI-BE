package com.fmi.domain.post.service;

import com.fmi.domain.Enum.Status;
import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
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
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));


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

    public ViewResponse getPostView(Long postId, UserDetails userDetails) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        String email=userDetails.getUsername();

        String viewSetKey = "post:view:set:" + postId;      // 어떤 유저가 조회했는지
        String viewCountKey = "post:view:count:" + postId;  // 게시글 조회수 누적

        Long added = stringRedisTemplate.opsForSet().add(viewSetKey, email);
        boolean newViewer = added != null && added > 0;
        if (newViewer) {
            stringRedisTemplate.opsForValue().increment(viewCountKey);
        }
        stringRedisTemplate.expire(viewSetKey, Duration.ofHours(1));

        Long currentViewCount = Optional.ofNullable(stringRedisTemplate.opsForValue().get(viewCountKey))
                .map(Long::parseLong)
                .orElse(post.getViewCnt());

        return postConverter.toViewResponse(postId,currentViewCount);

    }

    @Transactional(readOnly = true)
    public FilterResponse getPostsByFilter(PostFilterDto dto, Pageable pageable, Long cursorId) {

        Slice<Post> slice = postRepository.findPostsByFilters(dto, pageable, cursorId);

        return postConverter.toFilterResponse(slice);
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

}
