package com.fmi.domain.notice.comment.service;

import com.fmi.domain.notice.comment.data.NoticeComment;
import com.fmi.domain.notice.comment.data.NoticeCommentLike;
import com.fmi.domain.notice.comment.repository.NoticeCommentLikeRepository;
import com.fmi.domain.notice.comment.repository.NoticeCommentRepository;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeCommentLikeService {

    private final UserRepository userRepository;

    private final NoticeCommentRepository noticeCommentRepository;

    private final NoticeCommentLikeRepository noticeCommentLikeRepository;

    @Transactional
    public void addCommentLike(Long commentId, String email) {
        NoticeComment comment = noticeCommentRepository
                .findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        if (noticeCommentLikeRepository.findByUserAndComment(user, comment).isPresent()) {
            return;
        }

        noticeCommentLikeRepository.save(
                NoticeCommentLike.builder().user(user).comment(comment).build());
        noticeCommentRepository.incrementLikeCount(commentId);
    }

    @Transactional
    public void removeCommentLike(Long commentId, String email) {
        NoticeComment comment = noticeCommentRepository
                .findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        noticeCommentLikeRepository.findByUserAndComment(user, comment).ifPresent(like -> {
            noticeCommentLikeRepository.delete(like);
            noticeCommentRepository.decrementLikeCount(commentId);
        });
    }
}
