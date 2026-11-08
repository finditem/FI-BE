package com.fmi.domain.notice.notice.service;

import com.fmi.domain.notice.comment.repository.NoticeCommentImageRepository;
import com.fmi.domain.notice.comment.repository.NoticeCommentLikeRepository;
import com.fmi.domain.notice.comment.repository.NoticeCommentRepository;
import com.fmi.domain.notice.notice.converter.NoticeConverter;
import com.fmi.domain.notice.notice.data.Notice;
import com.fmi.domain.notice.notice.data.NoticeImage;
import com.fmi.domain.notice.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.notice.repository.NoticeImageRepository;
import com.fmi.domain.notice.notice.repository.NoticeLikeRepository;
import com.fmi.domain.notice.notice.repository.NoticeRepository;
import com.fmi.domain.notice.notice.web.dto.request.NoticeCreateRequest;
import com.fmi.domain.notice.notice.web.dto.request.NoticeUpdateRequest;
import com.fmi.domain.notice.notice.web.dto.response.NoticeResponse;
import com.fmi.domain.notification.service.NotificationBroadcastService;
import com.fmi.domain.post.post.data.enums.ImageType;
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
public class NoticeService {
    private final NotificationBroadcastService notificationBroadcastService;

    private final NoticeRepository noticeRepository;

    private final NoticeImageRepository noticeImageRepository;

    private final NoticeLikeRepository noticeLikeRepository;

    private final UserRepository userRepository;

    private final NoticeConverter noticeConverter;

    private final NoticeCommentImageRepository noticeCommentImageRepository;

    private final NoticeCommentRepository noticeCommentRepository;

    private final NoticeCommentLikeRepository noticeCommentLikeRepository;

    /**
     * 공지 생성 (관리자)
     */
    @Transactional
    public Long createNotice(NoticeCreateRequest request, String authorEmail) {
        User author = userRepository
                .findByEmail(authorEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory() == null ? NoticeCategory.GENERAL : request.getCategory())
                .pinned(Boolean.TRUE.equals(request.getPinned()))
                .draft(Boolean.TRUE.equals(request.getDraft()))
                .author(author)
                .build();

        Notice saved = noticeRepository.save(notice);

        // 이미지 저장
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ImageType type = (i == 0) ? ImageType.THUMBNAIL : ImageType.NORMAL;
                noticeImageRepository.save(
                        NoticeImage.create(request.getImageUrls().get(i), type, saved));
            }
        }

        // draft가 아닌 경우에만 알림 발송
        if (!Boolean.TRUE.equals(request.getDraft())) {
            notificationBroadcastService.broadcastNotice(request.getTitle(), request.getContent(), saved.getNoticeId());
        }

        return saved.getNoticeId();
    }

    /**
     * 공지사항 수정 (관리자)
     */
    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        Notice notice = noticeRepository
                .findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        boolean wasDraft = Boolean.TRUE.equals(notice.getDraft());

        String title =
                (request.getTitle() != null && !request.getTitle().isBlank()) ? request.getTitle() : notice.getTitle();
        String content = (request.getContent() != null && !request.getContent().isBlank())
                ? request.getContent()
                : notice.getContent();
        NoticeCategory category = request.getCategory() == null ? notice.getCategory() : request.getCategory();
        Boolean pinned = request.getPinned() == null ? notice.getPinned() : request.getPinned();
        notice.update(title, content, category, pinned, request.getDraft());

        // draft → 발행 전환 시 알림 발송
        if (wasDraft && Boolean.FALSE.equals(request.getDraft())) {
            notificationBroadcastService.broadcastNotice(notice.getTitle(), notice.getContent(), notice.getNoticeId());
        }

        // 이미지 업데이트 (전달된 경우에만)
        if (request.getImageUrls() != null) {
            noticeImageRepository.deleteAllByNotice(notice);
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ImageType type = (i == 0) ? ImageType.THUMBNAIL : ImageType.NORMAL;
                noticeImageRepository.save(
                        NoticeImage.create(request.getImageUrls().get(i), type, notice));
            }
        }

        List<NoticeImage> images = noticeImageRepository.findByNotice(notice);
        return noticeConverter.toResponseDTO(notice, images);
    }

    /**
     * 공지사항 삭제 (관리자)
     */
    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository
                .findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        noticeCommentLikeRepository.deleteAllByNoticeId(noticeId);
        noticeCommentImageRepository.deleteAllByNoticeId(noticeId);
        noticeLikeRepository.deleteByNoticeNoticeId(noticeId);
        noticeImageRepository.deleteAllByNotice(notice);
        noticeCommentRepository.deleteByNoticeNoticeId(noticeId);
        noticeRepository.delete(notice);
    }

    /**
     * 임시저장 공지사항 단건 조회 (관리자용 - 유저당 1건)
     */
    public NoticeResponse getDraftNotice(String authorEmail) {
        Notice draft = noticeRepository
                .findFirstByDraftTrueAndAuthorEmailOrderByUpdatedAtDesc(authorEmail)
                .orElse(null);
        if (draft == null) {
            return null;
        }
        List<NoticeImage> images = noticeImageRepository.findByNotice(draft);
        return noticeConverter.toResponseDTO(draft, images);
    }
}
