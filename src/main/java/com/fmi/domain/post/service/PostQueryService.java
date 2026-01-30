package com.fmi.domain.post.service;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.post.converter.PostConverter;
import com.fmi.domain.post.converter.PostImageConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.repository.PostImageRepository;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.web.dto.response.PostGetResponse;
import com.fmi.domain.post.web.dto.response.PostImageResponse;
import com.fmi.domain.post.web.dto.response.PostPageResponse;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.postfavorite.service.PostFavoriteService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.UserQueryService;
import com.fmi.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostQueryService {
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserQueryService userQueryService;
    private final PostFavoriteRepository postFavoriteRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final PostFavoriteService postFavoriteService;
    private final StringRedisTemplate stringRedisTemplate;

    // 게시글 단일 조회
    @Transactional
    public PostGetResponse getPost(Long postId, UserDetails userDetails, String clientIp) {
        Post post = findById(postId);

        Long chatRoomCount = chatRoomRepository.countByPostId(postId);
        Long userPostCount = postRepository.countByUserAndTemporarySaveFalse(post.getUser());

        List<PostImageResponse> imageList = PostImageConverter.toResponseList(postImageRepository.findByPost(post));

        boolean isFavorite = false;
        boolean canIncreaseViewCount;

        if (Objects.nonNull(userDetails)) {
            User user = userQueryService.findUser(userDetails.getUsername());

            PostFavorite favorite = postFavoriteRepository.findByUserAndPost(user, post)
                    .orElse(null);

            isFavorite = Objects.nonNull(favorite) && favorite.isFavorite();

            canIncreaseViewCount = canIncreaseViewCount(postId, user.getId());
        } else {
            canIncreaseViewCount = canIncreaseViewCount(postId, clientIp);
        }

        long viewCount = post.getViewCount();

        if (canIncreaseViewCount) {
            postRepository.increaseViewCount(post.getId());
            viewCount++;
        }

        long favoriteCount = postFavoriteService.countByPostAndIsFavoriteTrue(post);

        return PostConverter.toGetResponse(
                post,
                isFavorite,
                viewCount,
                chatRoomCount,
                post.isNew(),
                false,
                favoriteCount,
                userPostCount,
                imageList
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
                                                      LocalDate startDate,
                                                      LocalDate endDate,
                                                      SortType sortType,
                                                      Long cursor,
                                                      int size,
                                                      UserDetails userDetails) {

        User user = userQueryService.findUserIfNullReturnNull(userDetails);

        return postRepository.searchPostsByFiltersAndSort(
                postType,
                postStatus,
                category,
                address,
                startDate,
                endDate,
                sortType,
                cursor,
                size,
                user.getId());
    }

    public Post findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));
    }
}
