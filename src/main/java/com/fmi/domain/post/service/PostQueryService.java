package com.fmi.domain.post.service;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.fmi.domain.post.converter.util.PostConverter;
import com.fmi.domain.post.converter.util.PostImageConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.repository.PostImageRepository;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.web.dto.response.*;
import com.fmi.domain.post.web.dto.response.image.PostImageResponse;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.postfavorite.service.PostFavoriteService;
import com.fmi.domain.user.converter.UserConverter;
import com.fmi.domain.userblock.service.BlockService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.UserQueryService;
import com.fmi.utils.IpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostQueryService {
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserQueryService userQueryService;
    private final PostFavoriteRepository postFavoriteRepository;
    private final PostFavoriteService postFavoriteService;
    private final PostImageService postImageService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final BlockService blockService;


    // 게시글 단일 조회
    @Transactional
    public PostGetResponse getPost(Long postId, UserDetails userDetails, String clientIp) {
        Post post = findById(postId);

        List<PostImageResponse> imageList = PostImageConverter.toResponseList(postImageRepository.findByPost(post));

        boolean isFavorite = false;
        boolean canIncreaseViewCount;
        boolean isMine = false;
        if (Objects.nonNull(userDetails)) {
            User user = userQueryService.findUser(userDetails.getUsername());

            // 차단 체크 (양방향)
            if (blockService.isBlocked(user.getId(), post.getUser().getId())) {
                throw new GeneralException(ErrorStatus._POST_ACCESS_DENIED);
            }

            PostFavorite favorite = postFavoriteRepository.findByUserAndPost(user, post)
                    .orElse(null);

            isFavorite = Objects.nonNull(favorite) && favorite.isFavorite();

            canIncreaseViewCount = canIncreaseViewCount(postId, user.getId());

            if (Objects.equals(user.getId(), post.getUser().getId())) {
                isMine = true;
            }

        } else {
            canIncreaseViewCount = canIncreaseViewCount(postId, clientIp);
        }

        long viewCount = post.getViewCount();

        if (canIncreaseViewCount) {
            postRepository.increaseViewCount(post.getId());
            viewCount++;
        }

        long favoriteCount = postFavoriteService.countByPostAndIsFavoriteTrue(post);

        User user = post.getUser();
        long userPostCount = postRepository.countByUserAndTemporarySaveFalse(user);
        long chatRoomCount = chatRoomParticipantRepository.countActiveChatRoomsByUser(user);


        return PostConverter.toGetResponse(
                post,
                isFavorite,
                viewCount,
                post.isNew(),
                false,
                favoriteCount,
                isMine,
                imageList,
                UserConverter.toUserPostResponse(user, userPostCount, chatRoomCount)
        );
    }

    private boolean canIncreaseViewCount(Long postId, Long userId) {
        return isFirstToday(postId, "user", String.valueOf(userId));
    }

    private boolean canIncreaseViewCount(Long postId, String clientIp) {
        return isFirstToday(postId, "not-user", IpUtil.hashIp(clientIp));
    }

    private boolean isFirstToday(Long postId, String sic, String value) {
        String today = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        long ttlSeconds = secondsUntilMidnightSeoul();

        String redisKey = "post:view:" + postId + ":" + today + ":sic:" + sic + ":value:" + value;

        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", Duration.ofSeconds(ttlSeconds));

        return Boolean.TRUE.equals(first);
    }

    private long secondsUntilMidnightSeoul() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight).getSeconds();
    }

    @Transactional(readOnly = true)
    public PostPageResponse getPostListByFilterOrSort(PostType postType,
                                                      PostStatus postStatus,
                                                      Category category,
                                                      String address,
                                                      SortType sortType,
                                                      Long cursor,
                                                      int size,
                                                      UserDetails userDetails) {

        User user = userQueryService.findUserIfNullReturnNull(userDetails);
        Long userId = (Objects.isNull(user) ? null : user.getId());

        return postRepository.searchPostsByFiltersAndSort(
                postType,
                postStatus,
                category,
                address,
                sortType,
                cursor,
                size,
                userId);
    }

    @Transactional(readOnly = true)
    public PostPageResponse searchByKeyword(String keyword, Long cursor, int size, UserDetails userDetails) {
        User user = userQueryService.findUserIfNullReturnNull(userDetails);
        int limit = size + 1;

        keyword = sanitizeForLike(keyword);

        if (keyword.isBlank()) return new PostPageResponse(List.of(), 0L, null, false);

        long postCount = postRepository.countByKeyword(keyword);

        List<Post> fetched = postRepository.searchByKeywordWithCursor(keyword, cursor, limit);

        boolean hasNext = fetched.size() > size;

        if (hasNext) {
            fetched = fetched.subList(0, size);
        }

        Long nextCursor = fetched.isEmpty() ? null : fetched.get(fetched.size() - 1).getId();

        Map<Long, Long> favoriteCountMap = postFavoriteService.getFavoriteCountMap(fetched);
        Map<Long, Boolean> myFavoriteMap = postFavoriteService.getIsFavoriteMap(user, fetched);
        Map<Long, String> thumbnailMap = postImageService.findThumbnailUrlByPostList(fetched);
        Map<Long, Integer> postImageCountMap = postImageService.countImageByPostList(fetched);

        List<PostBriefResponse> responseList = fetched.stream()
                .map(post -> PostConverter.toPostBriefResponse(
                        post,
                        myFavoriteMap.getOrDefault(post.getId(), false),
                        thumbnailMap.getOrDefault(post.getId(), null),
                        favoriteCountMap.getOrDefault(post.getId(), 0L),
                        postImageCountMap.getOrDefault(post.getId(), 0)
                ))
                .toList();

        return new PostPageResponse(responseList, postCount, nextCursor, hasNext);
    }

    private String sanitizeForLike(String input) {
        if (input == null) return "";
        String s = Normalizer.normalize(input, Normalizer.Form.NFKC).trim();
        if (s.isEmpty()) return "";

        // 문자/숫자/한글/공백만 남김
        s = s.replaceAll("[^0-9A-Za-z가-힣\\s]", " ");
        s = s.replaceAll("\\s+", " ").trim();

        return s.length() > 100 ? s.substring(0, 100) : s;
    }

    @Transactional(readOnly = true)
    public Post findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<PostBriefResponse> getPostBriefResponseList(List<Post> postList, User user) {
        if (postList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Boolean> favoriteMap = postFavoriteService.getIsFavoriteMap(user, postList);
        Map<Long, Long> favoriteCountMap = postFavoriteService.getFavoriteCountMap(postList);
        Map<Long, String> thumbNailUrlMap = postImageService.findThumbnailUrlByPostList(postList);
        Map<Long, Integer> postImageCountMap = postImageService.countImageByPostList(postList);

        return postList.stream()
                .map(post ->
                        PostConverter.toPostBriefResponse(
                                post,
                                favoriteMap.getOrDefault(post.getId(), false),
                                thumbNailUrlMap.getOrDefault(post.getId(), ""),
                                favoriteCountMap.getOrDefault(post.getId(), 0L),
                                postImageCountMap.getOrDefault(post.getId(), 0)
                        )).toList();
    }

    @Transactional(readOnly = true)
    public PostShareResponse getSharePost(Long postId) {
        Post post = findById(postId);

        String thumbnailImageUrl = Optional.ofNullable(
                        postImageService.findThumbnailImage(post)
                ).map(PostImage::getImgUrl)
                .orElse(null);

        return PostConverter.toShareResponse(post, thumbnailImageUrl);
    }


    //즐찾 조회
    @Transactional(readOnly = true)
    public List<PostBriefResponse> getFavoritePost(UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());

        List<PostFavorite> favorites = postFavoriteRepository.findByUserAndIsFavoriteTrue(user);
        List<Post> posts = favorites.stream()
                .map(PostFavorite::getPost)
                .toList();

        return getPostBriefResponseList(posts, user);
    }

    @Transactional(readOnly = true)
    public List<PostSimilarResponse> getSimilarPostList(Long postId, UserDetails userDetails) {
        Post post = findById(postId);

        List<Post> postList = postRepository.findSimilarPosts(postId, 5);

        Map<Long, String> thumbnailUrlByPostList = postImageService.findThumbnailUrlByPostList(postList);
        Map<Long, Long> favoriteCountMap = postFavoriteService.getFavoriteCountMap(postList);

        Map<Long, Boolean> isFavoriteByPostId;
        if (userDetails != null) {
            User user = userQueryService.findUser(userDetails.getUsername());
            isFavoriteByPostId = postFavoriteService.getIsFavoriteMap(user, postList);
        } else {
            isFavoriteByPostId = postList.stream()
                    .collect(java.util.stream.Collectors.toMap(Post::getId, p -> false));
        }

        return postList.stream()
                .map(p -> new PostSimilarResponse(
                                p.getId(),
                                p.getTitle(),
                                thumbnailUrlByPostList.getOrDefault(p.getId(), null),
                                p.getAddress(),
                                favoriteCountMap.getOrDefault(p.getId(), 0L),
                                isFavoriteByPostId.getOrDefault(p.getId(), false),
                                p.getViewCount(),
                                p.getCreatedAt(),
                                p.getCategory()
                        )
                )
                .toList();
    }
}
