package com.fmi.domain.commentlike.service;


import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.commentlike.converter.CommentLikeConverter;
import com.fmi.domain.commentlike.data.CommentLike;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.commentlike.repository.CommentLikeRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskScheduler taskScheduler;
    private final UserRepository userRepository;
    private final CommentLikeConverter commentLikeConverter;

    private static final String LIKE_QUEUE_KEY = "comment:like:queue:";

    @Transactional
    public boolean toggleLike(Long commentId, UserDetails userDetails) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        CommentLike status = commentLikeRepository.findByUserAndComment(user, comment).orElse(null);

        boolean isLikedNow;

        if (status == null || !status.isLiked()) {
            // 좋아요 추가
            if (status == null) {
                commentLikeRepository.save(commentLikeConverter.toTrueEntity(user, comment));
                isLikedNow = true;
            } else {
                status.setLiked(true);
                isLikedNow=true;
            }
        } else {
            // 좋아요 상태 -> 좋아요 취소
            commentLikeRepository.delete(status);
            isLikedNow= false;
        }

        String key = "comment:like:queue:" + commentId;
        // 이미 Redis에 key가 있는지 확인
        boolean exists = Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));

        if (!comment.getUser().getId().equals(user.getId())) {
            if (!exists) {
                notificationService.createNotification(
                        comment.getUser(),
                        NotificationType.LIKE,
                        "댓글에 좋아요가 달렸습니다.",
                        String.format("%s님이 회원님의 댓글을 좋아했습니다.", user.getNickname()),
                        "COMMENT",
                        comment.getId()
                );
                taskScheduler.schedule(() -> sendAccumulatedLikeNotification(commentId),
                        Instant.now().plus(Duration.ofMinutes(3)));

                stringRedisTemplate.opsForSet().add(key, "_init");

            } else {
                stringRedisTemplate.opsForSet().add(key, String.valueOf(user.getId()));
            }
        }
        return isLikedNow;
    }


    private void sendAccumulatedLikeNotification(Long commentId) {

        String key = LIKE_QUEUE_KEY + commentId;
        Set<String> userIds = stringRedisTemplate.opsForSet().members(key);
        if (userIds == null || userIds.isEmpty()) return;

        userIds.remove("_init");

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            stringRedisTemplate.delete(key);
            return;
        }

        List<String> nicknames = userIds.stream()
                .map(id -> userRepository.findById(Long.parseLong(id))
                        .map(User::getNickname).orElse("알 수 없는 사용자"))
                .toList();

        String message;
        if (nicknames.size() == 1) {
            message = String.format("%s님이 회원님의 댓글을 좋아했습니다.", nicknames.get(0));
        } else {
            message = String.format("%s님 외 %d명이 회원님의 댓글을 좋아했습니다.",
                    nicknames.get(0), nicknames.size() - 1);
        }

        notificationService.createNotification(
                comment.getUser(),
                NotificationType.LIKE,
                "댓글에 좋아요가 달렸습니다.",
                message,
                "COMMENT",
                comment.getId()
        );

        stringRedisTemplate.delete(key);
    }
}