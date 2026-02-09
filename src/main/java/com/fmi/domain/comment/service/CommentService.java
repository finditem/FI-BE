package com.fmi.domain.comment.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.comment.converter.CommentConverter;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.comment.response.CommentSliceResponse;
import com.fmi.domain.comment.web.dto.CreateCommentDto;
import com.fmi.domain.comment.web.dto.request.CommentCreateRequest;
import com.fmi.domain.comment.web.dto.request.CommentUpdateRequest;
import com.fmi.domain.comment.web.dto.response.CommentCreateResponse;
import com.fmi.domain.comment.web.dto.response.CommentDeleteResponse;
import com.fmi.domain.comment.web.dto.response.CommentImageResponse;
import com.fmi.domain.commentlike.service.CommentLikeService;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.user.converter.UserConverter;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import com.fmi.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final PostQueryService postQueryService;
    private final UserQueryService userQueryService;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final CommentImageService commentImageService;
    private final CommentLikeService commentLikeService;

    @Transactional
    public CommentCreateResponse createCommentByPost(CommentCreateRequest request, UserDetails userDetails, Long postId, List<MultipartFile> images) {
        User user = userQueryService.findUser(userDetails.getUsername());
        Post post = postQueryService.findById(postId);

        Comment comment;
        if (Objects.nonNull(request.parentId())) {
            Comment parentComment = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_PARENT_NOT_FOUND));

            if (!Objects.equals(parentComment.getPost().getId(), post.getId())) {
                throw new IllegalArgumentException();
            }
            comment = Comment.createComment(user, post, request.content(), parentComment);

        } else {
            comment = Comment.createComment(user, post, request.content());
        }

        Comment saveComment = commentRepository.save(comment);

        List<CommentImageResponse> commentImageList = commentImageService.createCommentImageAtS3AndDB(images, saveComment);

        boolean isReply = (saveComment.getParent() != null);
        Set<Long> mentionedUserIds = handleMentions(saveComment, request);

        if (isReply) {
            notifyReply(saveComment.getParent(), user, request, post, mentionedUserIds);
        } else {
            notifyPostOwner(post, user, request, mentionedUserIds);
        }


        return CommentConverter.toCommentCreateResponse(comment, 0, true, UserConverter.toUserCommentResponse(user), commentImageList);
    }

    private Set<Long> handleMentions(Comment comment, CommentCreateRequest dto) {
        List<String> mentionedNicknames = extractMentions(dto.content());
        Set<Long> mentionedUserIds = new HashSet<>();

        for (String nickname : mentionedNicknames) {
            userRepository.findByNickname(nickname).ifPresent(mentionedUser -> {
//                log.info("멘션 대상 찾음: {}", mentionedUser.getNickname());
                if (!mentionedUser.getId().equals(comment.getUser().getId())) {//자신 거르기

                    notificationService.createNotification(
                            mentionedUser,
                            NotificationType.MENTION,
                            comment.getUser().getNickname() + "님이 멘션했습니다",
                            dto.content(),
                            ReferenceType.COMMENT,
                            comment.getId()
                    );

                    mentionedUserIds.add(mentionedUser.getId());

                    log.info("MENTION 알림 전송: to={}, type={}, commentId={}",
                            mentionedUser.getNickname(),
                            NotificationType.MENTION,
                            comment.getId());
                }
            });
        }
        return mentionedUserIds;
    }

    private List<String> extractMentions(String content) {

        if (content == null || content.isBlank() || !content.contains("@")) {
            return Collections.emptyList();
        }

        Set<String> mentions = new HashSet<>();

        Pattern pattern = Pattern.compile("(?<=\\s|^)@([A-Za-z0-9가-힣_.]+)(?=\\s|$)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }

        return new ArrayList<>(mentions);
    }


    private void notifyPostOwner(Post post, User commenter, CommentCreateRequest dto, Set<Long> mentionedUserIds) {

        Long postOwnerId = post.getUser().getId();

        if (postOwnerId.equals(commenter.getId()) || mentionedUserIds.contains(postOwnerId)) {
            log.info("이미 맨션알림을 보냈거나 자기가 자기게시글에 단 댓글 알림 생략");
            return;
        }

        notificationService.createNotification(
                post.getUser(),
                NotificationType.COMMENT,
                "새 댓글이 달렸습니다",
                dto.content(),
                ReferenceType.POST,
                post.getId()
        );
    }

    private void notifyReply(Comment parentComment, User replier, CommentCreateRequest dto, Post post, Set<Long> mentionedUserIds) {

        Long parentOwnerId = parentComment.getUser().getId();

        if (parentOwnerId.equals(replier.getId()) || mentionedUserIds.contains(parentOwnerId)) {
            log.info("대댓 유저와 부모댓글 유저가 같거나, 이미 맨션 보냈을 때 생략");
            return;
        }

        notificationService.createNotification(
                parentComment.getUser(),
                NotificationType.REPLY,
                "댓글에 답글이 달렸습니다",
                dto.content(),
                ReferenceType.POST,
                post.getId()
        );
    }

    @Transactional
    public CommentCreateResponse updateComment(CommentUpdateRequest request, UserDetails userDetails, Long commentId, List<MultipartFile> addImageList) {
        User user = userQueryService.findUser(userDetails.getUsername());
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new GeneralException(ErrorStatus._COMMENT_ALREADY_DELETED);
        }

        if (!Objects.equals(user.getId(), comment.getUser().getId())) {
            throw new GeneralException(ErrorStatus._COMMENT_ACCESS_DENIED);
        }

        comment.updateContent(request.content());
        commentImageService.createCommentImageAtS3AndDB(addImageList, comment);
        commentImageService.deleteCommentImageAtS3AndDBByImageIdList(request.deleteImageIds(), comment.getId());

        List<CommentImageResponse> imageList = commentImageService.findCommentImageListByComment(comment);

        int likeCount = commentLikeService.getLikeCount(comment.getId());


        return CommentConverter.toCommentCreateResponse(
                comment,
                likeCount,
                true,
                UserConverter.toUserCommentResponse(user),
                imageList);
    }

    @Transactional
    public CommentDeleteResponse deleteComment(Long commentId, UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new GeneralException(ErrorStatus._COMMENT_ALREADY_DELETED);
        }

        if (!Objects.equals(user.getId(), comment.getUser().getId())) {
            throw new GeneralException(ErrorStatus._COMMENT_ACCESS_DENIED);
        }

        comment.delete();

        commentImageService.deleteAllCommentImage(comment);
        commentLikeService.deleteByComment(comment);

        return CommentConverter.toDeleteResponse(comment);
    }

    private boolean isOwner(Comment comment, UserDetails userDetails) {
        return userDetails != null
                && comment.getUser() != null
                && comment.getUser().getEmail().equals(userDetails.getUsername());
    }

    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }

        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }

}

