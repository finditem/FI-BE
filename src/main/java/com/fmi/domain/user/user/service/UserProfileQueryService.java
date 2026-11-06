package com.fmi.domain.user.user.service;

import com.fmi.domain.post.comment.data.Comment;
import com.fmi.domain.post.comment.repository.CommentRepository;
import com.fmi.domain.post.comment.service.CommentImageService;
import com.fmi.domain.post.comment.service.CommentLikeService;
import com.fmi.domain.post.comment.web.dto.response.CommentImageResponse;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.PostFavorite;
import com.fmi.domain.post.post.repository.PostFavoriteRepository;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.post.post.service.PostQueryService;
import com.fmi.domain.post.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.post.review.data.ReviewPage;
import com.fmi.domain.post.review.service.ReviewQueryService;
import com.fmi.domain.post.review.web.dto.response.ReviewResponse;
import com.fmi.domain.user.block.repository.BlockedUserRepository;
import com.fmi.domain.user.user.converter.UserConverter;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.UserOtherPageType;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.domain.user.user.web.dto.response.UserCommentSummaryResponse;
import com.fmi.domain.user.user.web.dto.response.UserMetaResponse;
import com.fmi.domain.user.user.web.dto.response.UserOtherPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileQueryService {

    private final UserRepository userRepository;

    private final PostRepository postRepository;

    private final CommentRepository commentRepository;

    private final PostFavoriteRepository postFavoriteRepository;

    private final PostQueryService postQueryService;

    private final UserQueryService userQueryService;

    private final BlockedUserRepository blockedUserRepository;

    private final CommentLikeService commentLikeService;

    private final CommentImageService commentImageService;

    private final ReviewQueryService reviewQueryService;

    /**
     * 유저 메타데이터 조회 (OG 태그용)
     */
    @Transactional(readOnly = true)
    public UserMetaResponse getUserMeta(Long userId) {
        User user =
                userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        return new UserMetaResponse(user.getNickname());
    }

    /**
     * 타인 페이지 조회 (커서 기반 페이지네이션)
     * type 파라미터에 따라 조회할 데이터를 분리합니다.
     * - posts: 게시글만 조회
     * - comments: 댓글만 조회
     * - favorites: 즐겨찾기만 조회
     * - 미지정 또는 기본값: posts (게시글만 조회)
     */
    @Transactional(readOnly = true)
    public UserOtherPageResponse getOtherUserPage(
            Long userId, UserOtherPageType type, UserDetails userDetails, Long cursor, int size) {

        User targetUser =
                userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        User me = userQueryService.findUserIfNullReturnNull(userDetails);

        // size 상한 제한 (최소 1, 최대 50)
        size = Math.max(1, Math.min(size, 50));

        // type에 따라 필요한 데이터만 조회
        List<PostBriefResponse> posts = Collections.emptyList();
        List<UserCommentSummaryResponse> comments = Collections.emptyList();
        List<PostBriefResponse> favorites = Collections.emptyList();
        List<ReviewResponse> reviews = Collections.emptyList();
        Long nextCursor = null;
        boolean hasNext = false;

        UserOtherPageType resolvedType = type == null ? UserOtherPageType.POSTS : type;
        PageRequest pageRequest = PageRequest.of(0, size);

        switch (resolvedType) {
            case POSTS -> {
                Slice<Post> postSlice = (cursor == null)
                        ? postRepository.findByUserAndTemporarySaveFalseAndDeletedFalseOrderByIdDesc(
                                targetUser, pageRequest)
                        : postRepository.findByUserAndTemporarySaveFalseAndDeletedFalseAndIdLessThanOrderByIdDesc(
                                targetUser, cursor, pageRequest);

                List<Post> postList = postSlice.getContent();
                posts = postQueryService.getPostBriefResponseList(postList, me);

                hasNext = postSlice.hasNext();
                if (hasNext && !postList.isEmpty()) {
                    nextCursor = postList.get(postList.size() - 1).getId();
                }
            }
            case COMMENTS -> {
                Slice<Comment> commentSlice = (cursor == null)
                        ? commentRepository.findByUserOrderByIdDesc(targetUser, pageRequest)
                        : commentRepository.findByUserAndIdLessThanOrderByIdDesc(targetUser, cursor, pageRequest);

                List<Comment> commentList = commentSlice.getContent();

                Set<Long> excludedUserIds = getExcludedUserIds(me);
                if (!excludedUserIds.isEmpty()) {
                    commentList = commentList.stream()
                            .filter(c -> !excludedUserIds.contains(c.getUser().getId()))
                            .toList();
                }

                List<Long> commentIds = commentList.stream().map(Comment::getId).toList();
                Map<Long, Long> likeCountMap = commentLikeService.buildLikeCountMap(commentIds);
                Set<Long> myLikeSet = commentLikeService.buildMyLikeSet(commentIds, me);
                Map<Long, List<CommentImageResponse>> imageMap = commentImageService.buildImageMap(commentIds);

                comments = commentList.stream()
                        .map(comment -> UserConverter.toUserCommentSummaryResponse(
                                comment,
                                likeCountMap.getOrDefault(comment.getId(), 0L),
                                myLikeSet.contains(comment.getId()),
                                imageMap.getOrDefault(comment.getId(), List.of())))
                        .toList();

                hasNext = commentSlice.hasNext();
                if (hasNext && !commentList.isEmpty()) {
                    nextCursor = commentList.get(commentList.size() - 1).getId();
                }
            }

            case FAVORITES -> {
                Slice<PostFavorite> favoriteSlice = (cursor == null)
                        ? postFavoriteRepository.findByUserAndIsFavoriteTrueOrderByIdDesc(targetUser, pageRequest)
                        : postFavoriteRepository.findByUserAndIsFavoriteTrueAndIdLessThanOrderByIdDesc(
                                targetUser, cursor, pageRequest);

                List<Post> favoritePosts = favoriteSlice.getContent().stream()
                        .map(PostFavorite::getPost)
                        .toList();

                favorites = postQueryService.getPostBriefResponseList(favoritePosts, me);

                hasNext = favoriteSlice.hasNext();
                if (hasNext && !favoriteSlice.getContent().isEmpty()) {
                    nextCursor = favoriteSlice
                            .getContent()
                            .get(favoriteSlice.getContent().size() - 1)
                            .getFavorite_id();
                }
            }

            case REVIEWS -> {
                ReviewPage reviewPage = reviewQueryService.getProfileReceivedReviews(targetUser.getId(), cursor, size);
                reviews = ReviewResponse.listFrom(reviewPage);
                hasNext = reviewPage.hasNext();
                nextCursor = reviewPage.nextCursor();
            }
        }

        return UserConverter.toUserOtherPageResponse(
                targetUser, posts, comments, favorites, reviews, nextCursor, hasNext);
    }

    private Set<Long> getExcludedUserIds(User user) {
        if (user == null) return Set.of();
        Set<Long> excluded = new HashSet<>();
        excluded.addAll(blockedUserRepository.findBlockedUserIdsByBlockerId(user.getId()));
        excluded.addAll(blockedUserRepository.findBlockerIdsByBlockedId(user.getId()));
        return excluded;
    }
}
