package com.fmi.domain.notice.notice.service;

import com.fmi.domain.notice.notice.data.Notice;
import com.fmi.domain.notice.notice.data.NoticeLike;
import com.fmi.domain.notice.notice.repository.NoticeLikeRepository;
import com.fmi.domain.notice.notice.repository.NoticeRepository;
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
public class NoticeLikeService {

    private final NoticeRepository noticeRepository;

    private final NoticeLikeRepository noticeLikeRepository;

    private final UserRepository userRepository;

    /**
     * 공지사항 추천 추가
     */
    @Transactional
    public void addLike(Long noticeId, String email) {
        Notice notice = noticeRepository
                .findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        if (noticeLikeRepository.findByUserAndNotice(user, notice).isPresent()) {
            return;
        }

        noticeLikeRepository.save(NoticeLike.builder().user(user).notice(notice).build());
        noticeRepository.incrementLikeCount(noticeId);
    }

    /**
     * 공지사항 추천 삭제
     */
    @Transactional
    public void removeLike(Long noticeId, String email) {
        Notice notice = noticeRepository
                .findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        noticeLikeRepository.findByUserAndNotice(user, notice).ifPresent(like -> {
            noticeLikeRepository.delete(like);
            noticeRepository.decrementLikeCount(noticeId);
        });
    }
}
