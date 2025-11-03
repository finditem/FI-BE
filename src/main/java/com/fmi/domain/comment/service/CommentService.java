package com.fmi.domain.comment.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.comment.converter.CommentConverter;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.comment.response.CommentResponse;
import com.fmi.domain.comment.web.dto.CreateCommentDto;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentConverter commentConverter;
    private final com.fmi.domain.notification.service.NotificationService notificationService;

    @Transactional
    public CommentResponse createComment(CreateCommentDto dto, UserDetails userDetails, Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다"));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다"));

        Comment parentComment = null;

        if (dto.getParentId() != null) {
            parentComment = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글이 존재하지 않습니다"));
        }
0
        Comment comment = commentConverter.toCommentEntity(dto, user, post, parentComment);
        Comment savedComment = commentRepository.save(comment);

        if(!post.getUser().getId().equals(user.getId())){

            // DB 저장 + 커밋 후 웹소켓 전송
            notificationService.createNotification(
                    post.getUser(),
                    NotificationType.COMMENT,
                    "새 댓글이 달렸습니다",
                    dto.getContent(),
                    "POST",
                    post.getId()
            );
        }

        if (parentComment != null && !parentComment.getUser().getId().equals(user.getId())) {

            notificationService.createNotification(
                    parentComment.getUser(),
                    NotificationType.REPLY,
                    "댓글에 답글이 달렸습니다",
                    dto.getContent(),
                    "POST",
                    post.getId()
            );
        }


        return commentConverter.toCommentResponse(savedComment);
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

        return commentConverter.toCommentResponse(comment);
    }

    @Transactional
    public CommentResponse deleteComment(UserDetails userDetails, Long commentId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다"));

        if (!comment.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        commentRepository.delete(comment);

        return commentConverter.toCommentResponse(comment);
    }

    @Transactional
    public List<CommentResponse> getComment(Long postId) {

        List<Comment> comments = commentRepository.findByPostId(postId);

        return comments.stream()
                .map(commentConverter::toCommentResponse)
                .collect(Collectors.toList());
    }
}
