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

    @Transactional(readOnly = true)
    public CommentPageResponse getParentComments(Long postId, Long cursor, int size, UserDetails userDetails) {
        User user = userQueryService.findUserIfNullReturnNull(userDetails);
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Comment> fetched = (Objects.isNull(cursor))
                ? commentRepository.findParentComments(postId, pageable)
                : commentRepository.findParentCommentsWithCursor(postId, cursor, pageable);

        Set<Long> excludedUserIds = getExcludedUserIds(user);
        if (!excludedUserIds.isEmpty()) {
            fetched = fetched.stream()
                    .filter(c -> !excludedUserIds.contains(c.getUser().getId()))
                    .collect(Collectors.toList());
        }

        boolean hasNext = fetched.size() > size;
        if (hasNext) fetched = fetched.subList(0, size);

        Long nextCursor = fetched.isEmpty() ? null : fetched.get(fetched.size() - 1).getId();

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
                            null,
                            List.of(),
                            likeCountMap.getOrDefault(comment.getId(), 0L),
                            myLikePostSet.contains(comment.getId()),
                            isAuthor
                    );
                })
                .toList();

        return new CommentPageResponse(responses, hasNext, nextCursor);
    }

    @Transactional(readOnly = true)
    public CommentPageResponse getReplies(Long parentId, Long cursor, int size, UserDetails userDetails) {
        User user = userQueryService.findUserIfNullReturnNull(userDetails);
        Pageable pageable = PageRequest.of(0, size + 1);

        Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_PARENT_NOT_FOUND));

        List<Comment> fetched = (Objects.isNull(cursor))
                ? commentRepository.findReplies(parentComment.getId(), pageable)
                : commentRepository.findRepliesWithCursor(parentComment.getId(), cursor, pageable);

        Set<Long> excludedUserIds = getExcludedUserIds(user);
        if (!excludedUserIds.isEmpty()) {
            fetched = fetched.stream()
                    .filter(c -> !excludedUserIds.contains(c.getUser().getId()))
                    .collect(Collectors.toList());
        }

        boolean hasNext = fetched.size() > size;
        if (hasNext) fetched = fetched.subList(0, size);

        Long nextCursor = fetched.isEmpty() ? null : fetched.get(fetched.size() - 1).getId();

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
                            null,
                            List.of(),
                            likeCountMap.getOrDefault(reply.getId(), 0L),
                            myLikeSet.contains(reply.getId()),
                            isAuthor
                    );
                })
                .toList();

        return new CommentPageResponse(responses, hasNext, nextCursor);
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
