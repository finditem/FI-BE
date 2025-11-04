package com.fmi.domain.notice.service;

import com.fmi.domain.notice.converter.NoticeConverter;
import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.repository.NoticeRepository;
import com.fmi.domain.notice.web.dto.NoticeListDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import com.fmi.domain.notice.web.dto.NoticeCreateRequestDTO;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    
    private final NoticeRepository noticeRepository;
    private final NoticeConverter noticeConverter;
    private final NotificationService notificationService;
    
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
     */
    @Transactional
    public NoticeResponseDTO getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOTICE_NOT_FOUND));
        
        // 조회수 증가
        notice.increaseViewCount();
        
        return noticeConverter.toResponseDTO(notice);
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
}

