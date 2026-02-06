package com.fmi.domain.notice.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.notice.converter.NoticeConverter;
import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.NoticeImage;
import com.fmi.domain.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.repository.NoticeImageRepository;
import com.fmi.domain.notice.repository.NoticeRepository;
import com.fmi.domain.noticelike.data.NoticeLike;
import com.fmi.domain.noticelike.repository.NoticeLikeRepository;
import com.fmi.domain.notice.web.dto.NoticeListDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import com.fmi.domain.notice.web.dto.NoticeCreateRequestDTO;
import com.fmi.domain.notice.web.dto.NoticeUpdateRequestDTO;
import com.fmi.domain.noticecomment.repository.NoticeCommentRepository;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.data.ImageType;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    
    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final NoticeLikeRepository noticeLikeRepository;
    private final UserRepository userRepository;
    private final NoticeConverter noticeConverter;
    private final NotificationService notificationService;
    private final NoticeCommentRepository noticeCommentRepository;
    private final StringRedisTemplate stringRedisTemplate;
    
    /**
     * 공지사항 목록 조회 (카테고리/키워드 필터)
     */
    public Page<NoticeListDTO> getNoticeList(NoticeCategory category, String keyword, Pageable pageable) {
        Page<Notice> notices;
        if (keyword != null && !keyword.isBlank()) {
            notices = noticeRepository.searchNotices(category, keyword.trim(), pageable);
        } else if (category != null) {
            notices = noticeRepository.findByDraftFalseAndCategory(category, pageable);
        } else {
            notices = noticeRepository.findByDraftFalse(pageable);
        }

        // 썸네일 일괄 조회 (N+1 방지)
        List<Long> noticeIds = notices.getContent().stream()
                .map(Notice::getNoticeId)
                .toList();

        Map<Long, String> thumbnailMap = Map.of();
        if (!noticeIds.isEmpty()) {
            thumbnailMap = noticeImageRepository.findThumbnailsByNoticeIds(noticeIds).stream()
                    .collect(Collectors.toMap(
                            img -> img.getNotice().getNoticeId(),
                            NoticeImage::getImgUrl,
                            (a, b) -> a));
        }

        Map<Long, String> finalThumbnailMap = thumbnailMap;
        return notices.map(notice -> noticeConverter.toListDTO(
                notice,
                finalThumbnailMap.get(notice.getNoticeId())));
    }
    
    /**
     * 공지사항 상세 조회
     * @param noticeId 공지사항 ID
     * @param userIdentifier 사용자 식별자 (이메일 또는 IP 주소)
     */
    public NoticeResponseDTO getNoticeDetail(Long noticeId, String userIdentifier) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));
        
        // Redis를 사용하여 5분마다만 조회수 증가
        String viewSetKey = "notice:view:set:" + noticeId;      // 어떤 유저가 조회했는지
        String viewCountKey = "notice:view:count:" + noticeId;  // 공지사항 조회수 누적
        
        // Redis에 조회수가 없으면 DB 값으로 초기화
        String existingCount = stringRedisTemplate.opsForValue().get(viewCountKey);
        if (existingCount == null) {
            stringRedisTemplate.opsForValue().set(viewCountKey, String.valueOf(notice.getViewCount()));
            stringRedisTemplate.expire(viewCountKey, Duration.ofMinutes(5));
        }
        
        // 새로운 조회자인 경우에만 조회수 증가
        Long added = stringRedisTemplate.opsForSet().add(viewSetKey, userIdentifier);
        boolean newViewer = added != null && added > 0;
        if (newViewer) {
            stringRedisTemplate.opsForValue().increment(viewCountKey);
        }
        // 5분(300초) 후 만료
        stringRedisTemplate.expire(viewSetKey, Duration.ofMinutes(5));
        stringRedisTemplate.expire(viewCountKey, Duration.ofMinutes(5));
        
        // Redis에서 현재 조회수 가져오기
        Long currentViewCount = Optional.ofNullable(stringRedisTemplate.opsForValue().get(viewCountKey))
                .map(Long::parseLong)
                .orElse(notice.getViewCount().longValue());
        
        List<NoticeImage> images = noticeImageRepository.findByNotice(notice);
        NoticeResponseDTO response = noticeConverter.toResponseDTO(notice, images);
        // Redis 조회수로 덮어쓰기
        response.setViewCount(currentViewCount.intValue());

        return response;
    }

    /**
     * 공지 생성 (관리자)
     */
    @Transactional
    public Long createNotice(NoticeCreateRequestDTO request) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory() == null ? NoticeCategory.GENERAL : request.getCategory())
                .pinned(Boolean.TRUE.equals(request.getPinned()))
                .draft(Boolean.TRUE.equals(request.getDraft()))
                .build();

        Notice saved = noticeRepository.save(notice);

        // 이미지 저장
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ImageType type = (i == 0) ? ImageType.THUMBNAIL : ImageType.NORMAL;
                noticeImageRepository.save(NoticeImage.create(request.getImageUrls().get(i), type, saved));
            }
        }

        // draft가 아닌 경우에만 알림 발송
        if (!Boolean.TRUE.equals(request.getDraft())) {
            notificationService.broadcastNotice(
                    request.getTitle(),
                    request.getContent(),
                    saved.getNoticeId()
            );
        }

        return saved.getNoticeId();
    }

    /**
     * 공지사항 수정 (관리자)
     */
    @Transactional
    public NoticeResponseDTO updateNotice(Long noticeId, NoticeUpdateRequestDTO request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        NoticeCategory category = request.getCategory() == null ? notice.getCategory() : request.getCategory();
        Boolean pinned = request.getPinned() == null ? notice.getPinned() : request.getPinned();
        notice.update(request.getTitle(), request.getContent(), category, pinned);

        // 이미지 업데이트 (전달된 경우에만)
        if (request.getImageUrls() != null) {
            noticeImageRepository.deleteAllByNotice(notice);
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ImageType type = (i == 0) ? ImageType.THUMBNAIL : ImageType.NORMAL;
                noticeImageRepository.save(NoticeImage.create(request.getImageUrls().get(i), type, notice));
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
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        noticeLikeRepository.deleteByNoticeNoticeId(noticeId);
        noticeImageRepository.deleteAllByNotice(notice);
        noticeCommentRepository.deleteByNoticeNoticeId(noticeId);
        noticeRepository.delete(notice);
    }

    /**
     * 공지사항 추천 토글
     */
    @Transactional
    public boolean toggleLike(Long noticeId, String email) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        Optional<NoticeLike> existingLike = noticeLikeRepository.findByUserAndNotice(user, notice);

        if (existingLike.isPresent()) {
            noticeLikeRepository.delete(existingLike.get());
            notice.decreaseLikeCount();
            return false; // unliked
        } else {
            noticeLikeRepository.save(NoticeLike.builder()
                    .user(user)
                    .notice(notice)
                    .build());
            notice.increaseLikeCount();
            return true; // liked
        }
    }

    /**
     * 임시저장 공지사항 목록 (관리자용)
     */
    public Page<NoticeListDTO> getDraftNotices(Pageable pageable) {
        Page<Notice> drafts = noticeRepository.findByDraftTrue(pageable);
        return drafts.map(noticeConverter::toListDTO);
    }
}

