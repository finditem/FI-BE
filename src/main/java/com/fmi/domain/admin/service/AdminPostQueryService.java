package com.fmi.domain.admin.service;

import com.fmi.domain.post.post.converter.util.PostConverter;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.repository.PostContentPolicyRepository;
import com.fmi.domain.post.post.service.HotPostService;
import com.fmi.domain.post.post.service.PostFavoriteService;
import com.fmi.domain.post.post.service.PostImageService;
import com.fmi.domain.post.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.enums.SortType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPostQueryService {
    private final PostContentPolicyRepository postContentPolicyRepository;

    private final UserRepository userRepository;

    private final PostFavoriteService postFavoriteService;

    private final PostImageService postImageService;

    private final HotPostService hotPostService;

    public CursorPageResponse<PostBriefResponse> getContentPolicyPosts(
            SortType sortType,
            Category category,
            PostStatus postStatus,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            Long cursor,
            Long cursorViewCount,
            Long cursorFavCount,
            int size,
            String adminEmail) {

        size = Math.max(1, Math.min(size, 50));
        int fetchSize = size + 1;
        List<Post> posts;

        String categoryStr = category != null ? category.name() : null;
        String postStatusStr = postStatus != null ? postStatus.name() : null;
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        posts = switch (sortType) {
            case OLDEST ->
                postContentPolicyRepository.findContentPolicyPostsByOldest(
                        categoryStr, postStatusStr, trimmedKeyword, startDateTime, endDateTime, cursor, fetchSize);
            case MOST_VIEWED ->
                postContentPolicyRepository.findContentPolicyPostsByMostViewed(
                        categoryStr,
                        postStatusStr,
                        trimmedKeyword,
                        startDateTime,
                        endDateTime,
                        cursorViewCount,
                        cursor,
                        fetchSize);
            case MOST_FAVORITED ->
                postContentPolicyRepository.findContentPolicyPostsByMostFavorited(
                        categoryStr,
                        postStatusStr,
                        trimmedKeyword,
                        startDateTime,
                        endDateTime,
                        cursorFavCount,
                        cursor,
                        fetchSize);
            default ->
                postContentPolicyRepository.findContentPolicyPostsByLatest(
                        categoryStr, postStatusStr, trimmedKeyword, startDateTime, endDateTime, cursor, fetchSize);
        };

        boolean hasNext = posts.size() > size;
        List<Post> content = hasNext ? posts.subList(0, size) : posts;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        Map<Long, Long> favoriteCountMap = postFavoriteService.getFavoriteCountMap(content);
        Map<Long, String> thumbnailMap = postImageService.findThumbnailUrlByPostList(content);
        Map<Long, Integer> imageCountMap = postImageService.countImageByPostList(content);

        User adminUser = userRepository.findByEmail(adminEmail).orElse(null);
        Map<Long, Boolean> favoriteStatusMap =
                (adminUser != null) ? postFavoriteService.getIsFavoriteMap(adminUser, content) : Map.of();

        Set<Long> hotPostIds = hotPostService.resolveHotPostIdsForUser(null, postStatus, category, null, null, 5);

        List<PostBriefResponse> responseList = content.stream()
                .map(post -> PostConverter.toPostBriefResponse(
                        post,
                        favoriteStatusMap.getOrDefault(post.getId(), false),
                        thumbnailMap.getOrDefault(post.getId(), null),
                        favoriteCountMap.getOrDefault(post.getId(), 0L),
                        imageCountMap.getOrDefault(post.getId(), 0),
                        hotPostIds.contains(post.getId())))
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }
}
