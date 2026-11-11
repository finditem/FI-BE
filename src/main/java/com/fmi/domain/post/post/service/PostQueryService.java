package com.fmi.domain.post.post.service;

import com.fmi.domain.chat.room.repository.ChatRoomRepository;
import com.fmi.domain.post.comment.service.CommentQueryService;
import com.fmi.domain.post.post.converter.util.PostConverter;
import com.fmi.domain.post.post.converter.util.PostImageConverter;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.PostFavorite;
import com.fmi.domain.post.post.data.PostImage;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.repository.PostFavoriteRepository;
import com.fmi.domain.post.post.repository.PostImageRepository;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.post.post.repository.PostSearchRepository;
import com.fmi.domain.post.post.repository.redis.PostViewStore;
import com.fmi.domain.post.post.web.dto.response.*;
import com.fmi.domain.post.post.web.dto.response.PostImageResponse;
import com.fmi.domain.user.block.service.BlockService;
import com.fmi.domain.user.user.converter.UserConverter;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.enums.SortType;
import com.fmi.global.util.IpUtil;
import java.text.Normalizer;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostQueryService {
    private final PostSearchRepository postSearchRepository;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserQueryService userQueryService;
    private final PostFavoriteRepository postFavoriteRepository;
    private final PostFavoriteService postFavoriteService;
    private final PostImageService postImageService;
    private final PostViewStore postViewStore;
    private final ChatRoomRepository chatRoomRepository;
    private final CommentQueryService commentQueryService;
    private final BlockService blockService;
    private final HotPostService hotPostService;

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

            PostFavorite favorite =
                    postFavoriteRepository.findByUserAndPost(user, post).orElse(null);

            isFavorite = Objects.nonNull(favorite) && favorite.isFavorite();

            canIncreaseViewCount = canIncreaseViewCount(postId, user.getId());

            if (Objects.equals(user.getId(), post.getUser().getId())) {
                isMine = true;
            }

        } else {
            try {
                canIncreaseViewCount = canIncreaseViewCount(postId, clientIp);
            } catch (Exception e) {
                log.warn("비회원 조회수 처리 실패", e);
                canIncreaseViewCount = false;
            }
        }

        long viewCount = post.getViewCount();

        if (canIncreaseViewCount) {
            postRepository.increaseViewCount(post.getId());
            viewCount++;
        }

        long favoriteCount = postFavoriteService.countByPostAndIsFavoriteTrue(post);

        User user = post.getUser();
        long userPostCount = postRepository.countByUserAndTemporarySaveFalseAndDeletedFalse(user);
        long chatRoomCount = chatRoomRepository.countChatRoomsWithMessageByPostId(post.getId());

        return PostConverter.toGetResponse(
                post,
                isFavorite,
                viewCount,
                post.isNew(),
                false,
                favoriteCount,
                isMine,
                imageList,
                UserConverter.toUserPostResponse(user, userPostCount, chatRoomCount));
    }

    private boolean canIncreaseViewCount(Long postId, Long userId) {
        return postViewStore.isFirstToday(postId, "user", String.valueOf(userId));
    }

    private boolean canIncreaseViewCount(Long postId, String clientIp) {
        if (Objects.isNull(clientIp) || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            return false;
        }
        return postViewStore.isFirstToday(postId, "not-user", IpUtil.hashIp(clientIp));
    }

    @Transactional(readOnly = true)
    public PostPageResponse getPostListByFilterOrSort(
            PostType postType,
            PostStatus postStatus,
            Category category,
            String address,
            SortType sortType,
            Long cursor,
            int size,
            UserDetails userDetails) {

        User user = userQueryService.findUserIfNullReturnNull(userDetails);
        Long userId = (Objects.isNull(user) ? null : user.getId());

        Set<Long> hotIds = hotPostService.resolveHotPostIdsForUser(postType, postStatus, category, address, userId, 5);

        return postSearchRepository.searchPostsByFiltersAndSort(
                postType, postStatus, category, address, sortType, cursor, size, userId, hotIds);
    }

    @Transactional(readOnly = true)
    public PostPageResponse searchByKeyword(String keyword, Long cursor, int size, UserDetails userDetails) {
        User user = userQueryService.findUserIfNullReturnNull(userDetails);
        int limit = size + 1;

        keyword = sanitizeForLike(keyword);

        if (keyword.isBlank()) return new PostPageResponse(List.of(), 0L, null, false);

        long postCount = postSearchRepository.countByKeyword(keyword);

        List<Post> fetched = postSearchRepository.searchByKeywordWithCursor(keyword, cursor, limit);

        boolean hasNext = fetched.size() > size;

        if (hasNext) {
            fetched = fetched.subList(0, size);
        }

        Long nextCursor =
                fetched.isEmpty() ? null : fetched.get(fetched.size() - 1).getId();

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
                        postImageCountMap.getOrDefault(post.getId(), 0)))
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
        return postRepository
                .findByIdAndDeletedFalse(postId)
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
                .map(post -> PostConverter.toPostBriefResponse(
                        post,
                        favoriteMap.getOrDefault(post.getId(), false),
                        thumbNailUrlMap.getOrDefault(post.getId(), ""),
                        favoriteCountMap.getOrDefault(post.getId(), 0L),
                        postImageCountMap.getOrDefault(post.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PostShareResponse getSharePost(Long postId) {
        Post post = findById(postId);

        String thumbnailImageUrl = Optional.ofNullable(postImageService.findThumbnailImage(post))
                .map(PostImage::getImgUrl)
                .orElse(null);

        Long likeCount = postFavoriteService.countByPostAndIsFavoriteTrue(post);
        Long commentCount = commentQueryService.countByPost(post);

        return PostConverter.toShareResponse(post, thumbnailImageUrl, likeCount, commentCount);
    }

    @Transactional(readOnly = true)
    public List<PostSimilarResponse> getSimilarPostList(Long postId, UserDetails userDetails) {
        Post post = findById(postId);

        List<Post> postList = postSearchRepository.findSimilarPosts(post.getId(), 5);

        Map<Long, String> thumbnailUrlByPostList = postImageService.findThumbnailUrlByPostList(postList);
        Map<Long, Long> favoriteCountMap = postFavoriteService.getFavoriteCountMap(postList);

        Map<Long, Boolean> isFavoriteByPostId;
        if (userDetails != null) {
            User user = userQueryService.findUser(userDetails.getUsername());
            isFavoriteByPostId = postFavoriteService.getIsFavoriteMap(user, postList);
        } else {
            isFavoriteByPostId = postList.stream().collect(java.util.stream.Collectors.toMap(Post::getId, p -> false));
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
                        p.getCategory()))
                .toList();
    }
}
