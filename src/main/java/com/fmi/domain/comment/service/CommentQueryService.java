package com.fmi.domain.comment.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.converter.CommentConverter;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.comment.web.dto.response.CommentImageResponse;
import com.fmi.domain.comment.web.dto.response.CommentPageResponse;
import com.fmi.domain.comment.web.dto.response.CommentResponse;
import com.fmi.domain.commentlike.service.CommentLikeService;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.user.converter.UserConverter;
import com.fmi.domain.userblock.repository.BlockedUserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentQueryService {
    private final CommentRepository commentRepository;
    private final CommentImageService commentImageService;
    private final UserQueryService userQueryService;
    private final CommentLikeService commentLikeService;
    private final BlockedUserRepository blockedUserRepository;

    private static final int PAGE_SIZE = 10;

    @Transactional(readOnly = true)
    public CommentPageResponse getParentComments(Long postId, int page, UserDetails userDetails) {
        User user = userQueryService.findUserIfNullReturnNull(userDetails);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        Set<Long> excludedUserIds = getExcludedUserIds(user);
        boolean excludedEmpty = (excludedUserIds == null || excludedUserIds.isEmpty());

        List<Comment> fetched = commentRepository.findParentComments(
                postId,
                excludedEmpty ? Set.of(0L) : excludedUserIds,
                excludedEmpty,
                pageable
        );

        boolean hasNext = fetched.size() > PAGE_SIZE;
        if (hasNext) fetched = fetched.subList(0, PAGE_SIZE);

        Integer nextPage = hasNext ? page + 1 : null;

        long totalCount = commentRepository.countParentComments(
                postId,
                excludedEmpty ? Set.of(0L) : excludedUserIds,
                excludedEmpty
        );

        long shown = ((long) page * PAGE_SIZE) + fetched.size();
        long remainingCount = Math.max(0, totalCount - shown);

        List<Long> parentIds = fetched.stream()
                .map(Comment::getId)
                .toList();

        Map<Long, Long> replyCountMap = buildReplyCountMap(parentIds);
        Map<Long, List<CommentImageResponse>> imageMap = commentImageService.buildImageMap(parentIds);

        Map<Long, Long> likeCountMap = commentLikeService.buildLikeCountMap(parentIds);
        Set<Long> myLikePostSet = commentLikeService.buildMyLikeSet(parentIds, user);

        List<CommentResponse> responses = fetched.stream()
                .map(comment -> {
                    boolean isAuthor = Objects.nonNull(user) && Objects.equals(comment.getUser().getId(), user.getId());
                    return CommentConverter.toCommentResponse(
                            comment,
                            UserConverter.toUserCommentResponse(comment.getUser()),
                            imageMap.getOrDefault(comment.getId(), List.of()),
                            replyCountMap.getOrDefault(comment.getId(), 0L),
                            likeCountMap.getOrDefault(comment.getId(), 0L),
                            myLikePostSet.contains(comment.getId()),
                            isAuthor
                    );
                })
                .toList();

        return new CommentPageResponse(responses, hasNext, nextPage, remainingCount);
    }

    @Transactional(readOnly = true)
    public CommentPageResponse getReplies(Long parentId, int page, UserDetails userDetails) {
        User user = userQueryService.findUserIfNullReturnNull(userDetails);

        Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_PARENT_NOT_FOUND));

        Set<Long> excludedUserIds = getExcludedUserIds(user);
        boolean excludedEmpty = excludedUserIds == null || excludedUserIds.isEmpty();

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        List<Comment> fetched = commentRepository.findReplies(
                parentComment.getId(),
                excludedEmpty ? Set.of(0L) : excludedUserIds,
                excludedEmpty,
                pageable
        );

        boolean hasNext = fetched.size() > PAGE_SIZE;
        if (hasNext) fetched = fetched.subList(0, PAGE_SIZE);

        Integer nextPage = hasNext ? page + 1 : null;

        long totalCount = commentRepository.countReplies(
                parentComment.getId(),
                excludedEmpty ? Set.of(0L) : excludedUserIds,
                excludedEmpty
        );

        long shown = (long) page * PAGE_SIZE + fetched.size();
        long remainingCount = Math.max(0, totalCount - shown);

        List<Long> replyIds = fetched.stream()
                .map(Comment::getId)
                .toList();

        Map<Long, Long> replyCountMap = buildReplyCountMap(replyIds);
        Map<Long, List<CommentImageResponse>> imageMap = commentImageService.buildImageMap(replyIds);

        Map<Long, Long> likeCountMap = commentLikeService.buildLikeCountMap(replyIds);
        Set<Long> myLikeSet = commentLikeService.buildMyLikeSet(replyIds, user);

        List<CommentResponse> responses = fetched.stream()
                .map(reply -> {
                    boolean isAuthor = Objects.nonNull(user) && Objects.equals(reply.getUser().getId(), user.getId());
                    return CommentConverter.toCommentResponse(
                            reply,
                            UserConverter.toUserCommentResponse(reply.getUser()),
                            imageMap.getOrDefault(reply.getId(), List.of()),
                            (reply.getDepth() < 2) ? replyCountMap.getOrDefault(reply.getId(), 0L) : 0L,
                            likeCountMap.getOrDefault(reply.getId(), 0L),
                            myLikeSet.contains(reply.getId()),
                            isAuthor
                    );
                })
                .toList();

        return new CommentPageResponse(responses, hasNext, nextPage, remainingCount);
    }


    private Set<Long> getExcludedUserIds(User user) {
        if (user == null) return Set.of();
        Set<Long> excluded = new HashSet<>();
        excluded.addAll(blockedUserRepository.findBlockedUserIdsByBlockerId(user.getId()));
        excluded.addAll(blockedUserRepository.findBlockerIdsByBlockedId(user.getId()));
        return excluded;
    }

    private Map<Long, Long> buildReplyCountMap(List<Long> parentIds) {
        if (parentIds.isEmpty()) return Map.of();

        return commentRepository.countRepliesByParentIds(parentIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    public Long countByPost(Post post) {
        return commentRepository.countByPostId(post.getId());
    }

    public Map<Long, Long> buildCountByPost(List<Post> postList) {
        if (Objects.isNull(postList) || postList.isEmpty()) return Map.of();

        List<Long> postIdList = postList.stream().map(Post::getId).toList();

        return commentRepository.countCommentsGroupByPostId(postIdList).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }


}
