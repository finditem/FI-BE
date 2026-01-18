package com.fmi.domain.notice.service;

import com.fmi.domain.notice.converter.NoticeConverter;
import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.repository.NoticeRepository;
import com.fmi.domain.notice.web.dto.NoticeListDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import com.fmi.domain.notice.web.dto.NoticeCreateRequestDTO;
import com.fmi.domain.notice.web.dto.NoticeUpdateRequestDTO;
import com.fmi.domain.noticecomment.repository.NoticeCommentRepository;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    
    private final NoticeRepository noticeRepository;
    private final NoticeConverter noticeConverter;
    private final NotificationService notificationService;
    private final NoticeCommentRepository noticeCommentRepository;
    private final StringRedisTemplate stringRedisTemplate;
    
    /**
     * 공지사항 목록 조회
     */
    public Page<NoticeListDTO> getNoticeList(Pageable pageable) {
        Page<Notice> notices = noticeRepository.findAll(pageable);
        return notices.map(noticeConverter::toListDTO);
    }
    
    /**
     * 카테고리별 공지사항 목록 조회
     */
    public Page<NoticeListDTO> getNoticeListByCategory(NoticeCategory category, Pageable pageable) {
        Page<Notice> notices = noticeRepository.findByCategory(category, pageable);
        return notices.map(noticeConverter::toListDTO);
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
        
        NoticeResponseDTO response = noticeConverter.toResponseDTO(notice);
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
                .build();

        Notice saved = noticeRepository.save(notice);

        // 전체 브로드캐스트(설정 반영)
        notificationService.broadcastNotice(
                request.getTitle(),
                request.getContent(),
                saved.getNoticeId()
        );

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

        return noticeConverter.toResponseDTO(notice);
    }

    /**
     * 공지사항 삭제 (관리자)
     */
    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        noticeCommentRepository.deleteByNoticeNoticeId(noticeId);
        noticeRepository.delete(notice);
    }
}

