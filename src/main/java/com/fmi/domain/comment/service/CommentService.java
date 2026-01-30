package com.fmi.domain.comment.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.comment.converter.CommentConverter;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.comment.response.CommentResponse;
import com.fmi.domain.comment.response.CommentSliceResponse;
import com.fmi.domain.comment.web.dto.CreateCommentDto;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentConverter commentConverter;
    private final NotificationService notificationService;
    private final S3Service s3Service;

    @Transactional
    public CommentResponse createComment(CreateCommentDto dto, UserDetails userDetails, Long postId,List<MultipartFile> images) {


        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다"));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다"));

        List<String> s3Urls = (images == null || images.isEmpty())
                ? new ArrayList<>()
                : s3Service.upload(images);


        Comment parentComment = null;

        if (dto.getParentId() != null) {
            parentComment = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글이 존재하지 않습니다"));
        }

        Comment comment = commentConverter.toCommentEntity(dto, user, post, parentComment);
        Comment savedComment = commentRepository.save(comment);

//        List<PostImage> postImages = CommentConverter.toPostImageEntities(post, s3Urls);
//
//        if (!postImages.isEmpty()) {
//            commentImage.saveAll(postImages);
//            post.setImages(postImages);
//        }

//        post.increaseCommentCount();
        boolean isReply = parentComment != null;

        Set<Long> mentionedUserIds = handleMentions(savedComment, dto);

        if (isReply) {
            notifyReply(parentComment, user, dto, post, mentionedUserIds);
        } else {
            notifyPostOwner(post, user, dto, mentionedUserIds);
        }

        return toResponse(savedComment, userDetails);
    }

    private Set<Long> handleMentions(Comment comment, CreateCommentDto dto) {
        List<String> mentionedNicknames = extractMentions(dto.getContent());
        Set<Long> mentionedUserIds = new HashSet<>();

        for (String nickname : mentionedNicknames) {
            userRepository.findByNickname(nickname).ifPresent(mentionedUser -> {
//                log.info("멘션 대상 찾음: {}", mentionedUser.getNickname());
                if (!mentionedUser.getId().equals(comment.getUser().getId())) {//자신 거르기

                    notificationService.createNotification(
                            mentionedUser,
                            NotificationType.MENTION,
                            comment.getUser().getNickname() + "님이 멘션했습니다",
                            dto.getContent(),
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


    private void notifyPostOwner(Post post, User commenter, CreateCommentDto dto, Set<Long> mentionedUserIds) {

        Long postOwnerId = post.getUser().getId();

        if (postOwnerId.equals(commenter.getId()) || mentionedUserIds.contains(postOwnerId)) {
            log.info("이미 맨션알림을 보냈거나 자기가 자기게시글에 단 댓글 알림 생략");
            return;
        }

        notificationService.createNotification(
                post.getUser(),
                NotificationType.COMMENT,
                "새 댓글이 달렸습니다",
                dto.getContent(),
                ReferenceType.POST,
                post.getId()
        );
    }

    private void notifyReply(Comment parentComment, User replier, CreateCommentDto dto, Post post, Set<Long> mentionedUserIds) {

        Long parentOwnerId = parentComment.getUser().getId();

        if (parentOwnerId.equals(replier.getId()) || mentionedUserIds.contains(parentOwnerId)) {
            log.info("대댓 유저와 부모댓글 유저가 같거나, 이미 맨션 보냈을 때 생략");
            return;
        }

        notificationService.createNotification(
                parentComment.getUser(),
                NotificationType.REPLY,
                "댓글에 답글이 달렸습니다",
                dto.getContent(),
                ReferenceType.POST,
                post.getId()
        );
    }

    @Transactional
    public CommentResponse updateComment(CreateCommentDto dto, UserDetails userDetails, Long commentId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다"));

        if (!comment.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        comment.setContent(dto.getContent());
        comment.setUpdatedAt(LocalDateTime.now());

        return toResponse(comment, userDetails);
    }

    @Transactional
    public CommentResponse deleteComment(UserDetails userDetails, Long commentId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다"));

        // 작성자이거나 관리자인 경우 삭제 가능
        boolean isOwner = comment.getUser().getEmail().equals(userDetails.getUsername());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("작성자 또는 관리자만 삭제할 수 있습니다.");
        }

//        comment.getPost().decreaseCommentCount(); // 댓글 수 감소

        commentRepository.delete(comment);

        return toResponse(comment, userDetails);
    }


    @Transactional(readOnly = true)
    public CommentSliceResponse getComments(Long postId, Long cursor, int size, UserDetails userDetails) {

        Slice<Comment> comments;

        if (cursor == null) {
            comments = commentRepository.findTopByPostIdOrderByIdDesc(postId, Pageable.ofSize(size));
        } else {
            comments = commentRepository.findByPostIdAndIdLessThanOrderByIdDesc(postId, cursor, Pageable.ofSize(size));
        }

        Long nextCursor  = comments.hasNext()
                ? comments.getContent().get(comments.getContent().size() - 1).getId()
                : null;


        List<CommentResponse> result = comments.stream()
                .limit(size)
                .map(comment -> toResponse(comment, userDetails))
                .toList();

        return new CommentSliceResponse(result, comments.hasNext(), nextCursor);
    }

    private CommentResponse toResponse(Comment comment, UserDetails userDetails) {
        boolean isOwner = isOwner(comment, userDetails);
        boolean isAdmin = isAdmin(userDetails);

        return commentConverter.toCommentResponse(comment, isOwner, isOwner || isAdmin);
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

